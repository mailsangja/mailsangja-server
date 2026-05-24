package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.common.util.LabelRuleValidator;
import com.mailsangja.core.dto.label.LabelCreateRequest;
import com.mailsangja.core.dto.label.LabelDetailResponse;
import com.mailsangja.core.dto.label.LabelListResponse;
import com.mailsangja.core.dto.label.LabelRuleUpdateRequest;
import com.mailsangja.core.dto.label.LabelUpdateRequest;
import com.mailsangja.core.service.label.LabelCommandService;
import com.mailsangja.core.service.label.LabelQueryService;
import com.mailsangja.core.service.label.LabelReclassifyPendingJobStore;
import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.common.label.NotificationPolicy;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mailsangja.db.common.label.LabelRule.Field.SUBJECT;
import static com.mailsangja.db.common.label.LabelRule.Operator.CONTAINS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelFacadeTest {

    @Mock
    private LabelQueryService labelQueryService;

    @Mock
    private LabelCommandService labelCommandService;

    @Mock
    private LabelReclassifyPendingJobStore pendingJobStore;

    @Mock
    private LabelRuleValidator labelRuleValidator;

    @InjectMocks
    private LabelFacade labelFacade;

    @Test
    void 라벨목록조회_미읽은스레드수매핑및누락시_0반환() {
        User user = user();
        Label first = label("업무", 1);
        Label second = label("개인", 2);
        when(labelQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(first, second));
        when(labelQueryService.findUnreadThreadCountsByUserId(user.getId())).thenReturn(Map.of(first.getId(), 7L));

        List<LabelListResponse> responses = labelFacade.getLabels(user);

        assertEquals(2, responses.size());
        assertEquals(first.getId(), responses.get(0).id());
        assertEquals(first.isSensitive(), responses.get(0).isSensitive());
        assertEquals(7L, responses.get(0).unreadThreadCount());
        assertEquals(second.getId(), responses.get(1).id());
        assertEquals(second.isSensitive(), responses.get(1).isSensitive());
        assertEquals(0L, responses.get(1).unreadThreadCount());
    }

    @Test
    void 라벨상세조회_라벨의규칙반환() {
        User user = user();
        LabelRule rule = rule("invoice");
        Label label = label("업무", 1, rule);
        when(labelQueryService.findActiveByIdAndUserId(label.getId(), user.getId())).thenReturn(label);

        LabelDetailResponse response = labelFacade.getLabelDetail(user, label.getId());

        assertEquals(label.getId(), response.id());
        assertEquals(label.isSensitive(), response.isSensitive());
        assertSame(rule, response.rule());
    }

    @Test
    void 라벨생성_생성전이름중복시_예외발생하고생성안됨() {
        User user = user();
        LabelCreateRequest request = new LabelCreateRequest("  업무  ", "#3366FF", NotificationPolicy.INHERIT, 1, false, null);
        when(labelQueryService.existsByUserIdAndName(user.getId(), "업무")).thenReturn(true);

        LabelException exception = assertThrows(LabelException.class, () -> labelFacade.createLabel(user, request));

        assertEquals(LabelErrorCode.LABEL_NAME_DUPLICATE, exception.getErrorCode());
        verify(labelCommandService, never()).create(user, request);
    }

    @Test
    void 라벨생성_규칙있을때_규칙검증및PendingJob병합() {
        User user = user();
        LabelRule rule = rule("invoice");
        LabelCreateRequest request = new LabelCreateRequest("업무", "#3366FF", NotificationPolicy.URGENT, 1, true, rule);
        Label saved = label("업무", 1, rule);
        when(labelQueryService.existsByUserIdAndName(user.getId(), "업무")).thenReturn(false);
        when(labelCommandService.create(user, request)).thenReturn(saved);

        LabelDetailResponse response = labelFacade.createLabel(user, request);

        verify(labelRuleValidator).validate(rule);
        verify(pendingJobStore).checkRateLimit(user.getId());
        verify(pendingJobStore).mergePendingJob(user.getId(), saved.getId());
        assertEquals(saved.getId(), response.id());
        assertEquals(saved.isSensitive(), response.isSensitive());
        assertSame(rule, response.rule());
    }

    @Test
    void 라벨생성_규칙없을때_PendingJob생성안됨() {
        User user = user();
        LabelCreateRequest request = new LabelCreateRequest("업무", "#3366FF", NotificationPolicy.INHERIT, 1, false, null);
        Label saved = label("업무", 1, null);
        when(labelCommandService.create(user, request)).thenReturn(saved);

        labelFacade.createLabel(user, request);

        verify(labelRuleValidator).validate(null);
        verify(pendingJobStore, never()).checkRateLimit(user.getId());
        verify(pendingJobStore, never()).mergePendingJob(user.getId(), saved.getId());
    }

    @Test
    void 라벨생성_DB중복시_이름중복예외발생() {
        User user = user();
        LabelCreateRequest request = new LabelCreateRequest("업무", "#3366FF", NotificationPolicy.INHERIT, 1, false, null);
        when(labelCommandService.create(user, request)).thenThrow(new DataIntegrityViolationException("duplicate"));

        LabelException exception = assertThrows(LabelException.class, () -> labelFacade.createLabel(user, request));

        assertEquals(LabelErrorCode.LABEL_NAME_DUPLICATE, exception.getErrorCode());
    }

    @Test
    void 라벨수정_이름이공백이면_이름공백예외발생() {
        User user = user();
        Label label = label("기존", 1);
        LabelUpdateRequest request = new LabelUpdateRequest("  ", null, null, null, null);
        when(labelQueryService.findActiveByIdAndUserId(label.getId(), user.getId())).thenReturn(label);

        LabelException exception = assertThrows(LabelException.class, () -> labelFacade.updateLabel(user, label.getId(), request));

        assertEquals(LabelErrorCode.LABEL_NAME_BLANK, exception.getErrorCode());
        verify(labelCommandService, never()).update(label, request);
    }

    @Test
    void 라벨생성_RateLimit초과시_예외전파되고PendingJob병합안됨() {
        User user = user();
        LabelRule rule = rule("invoice");
        LabelCreateRequest request = new LabelCreateRequest("업무", "#3366FF", NotificationPolicy.URGENT, 1, true, rule);
        Label saved = label("업무", 1, rule);
        when(labelQueryService.existsByUserIdAndName(user.getId(), "업무")).thenReturn(false);
        when(labelCommandService.create(user, request)).thenReturn(saved);
        doThrow(new LabelException(LabelErrorCode.LABEL_RECLASSIFY_RATE_LIMITED, 45L))
                .when(pendingJobStore).checkRateLimit(user.getId());

        LabelException exception = assertThrows(LabelException.class, () -> labelFacade.createLabel(user, request));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_RATE_LIMITED, exception.getErrorCode());
        assertEquals(45L, exception.getRetryAfterSeconds());
        verify(pendingJobStore, never()).mergePendingJob(any(), any());
    }

    @Test
    void 라벨규칙수정_규칙업데이트및PendingJob병합() {
        User user = user();
        LabelRule rule = rule("invoice");
        Label label = label("업무", 1, null);
        Label updated = label("업무", 1, rule);
        LabelRuleUpdateRequest request = new LabelRuleUpdateRequest(rule);
        when(labelQueryService.findActiveByIdAndUserId(label.getId(), user.getId())).thenReturn(label);
        when(labelCommandService.updateRule(label, rule)).thenReturn(updated);

        LabelDetailResponse response = labelFacade.updateLabelRule(user, label.getId(), request);

        verify(labelRuleValidator).validate(rule);
        verify(pendingJobStore).checkRateLimit(user.getId());
        verify(pendingJobStore).mergePendingJob(user.getId(), updated.getId());
        assertSame(rule, response.rule());
    }

    @Test
    void updateLabelRule_rateLimit초과_예외전파되고PendingJob병합안됨() {
        User user = user();
        LabelRule rule = rule("invoice");
        Label label = label("업무", 1, null);
        Label updated = label("업무", 1, rule);
        LabelRuleUpdateRequest request = new LabelRuleUpdateRequest(rule);
        when(labelQueryService.findActiveByIdAndUserId(label.getId(), user.getId())).thenReturn(label);
        when(labelCommandService.updateRule(label, rule)).thenReturn(updated);
        doThrow(new LabelException(LabelErrorCode.LABEL_RECLASSIFY_RATE_LIMITED, 30L))
                .when(pendingJobStore).checkRateLimit(user.getId());

        LabelException exception = assertThrows(LabelException.class,
                () -> labelFacade.updateLabelRule(user, label.getId(), request));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_RATE_LIMITED, exception.getErrorCode());
        assertEquals(30L, exception.getRetryAfterSeconds());
        verify(pendingJobStore, never()).mergePendingJob(any(), any());
    }

    @Test
    void 라벨제안단건조회_제안의규칙반환() {
        User user = user();
        LabelRule rule = rule("invoice");
        Label suggestion = label("업무", 1, rule);
        when(labelQueryService.findSuggestionByIdAndUserId(suggestion.getId(), user.getId())).thenReturn(suggestion);

        LabelDetailResponse response = labelFacade.getSuggestionDetail(user, suggestion.getId());

        assertEquals(suggestion.getId(), response.id());
        assertSame(rule, response.rule());
    }

    @Test
    void 라벨제안단건조회_규칙없는제안_null규칙반환() {
        User user = user();
        Label suggestion = label("업무", 1, null);
        when(labelQueryService.findSuggestionByIdAndUserId(suggestion.getId(), user.getId())).thenReturn(suggestion);

        LabelDetailResponse response = labelFacade.getSuggestionDetail(user, suggestion.getId());

        assertEquals(suggestion.getId(), response.id());
        assertEquals(null, response.rule());
    }

    @Test
    void 라벨삭제_CommandService에위임() {
        User user = user();
        Label label = label("삭제", 1);
        when(labelQueryService.findActiveByIdAndUserId(label.getId(), user.getId())).thenReturn(label);

        labelFacade.deleteLabel(user, label.getId());

        verify(labelCommandService).delete(label);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .build();
    }

    private Label label(String name, int order) {
        return label(name, order, null);
    }

    private Label label(String name, int order, LabelRule rule) {
        return Label.builder()
                .id(UUID.randomUUID())
                .name(name)
                .colorCode("#3366FF")
                .notificationPolicy(NotificationPolicy.INHERIT)
                .displayOrder(order)
                .rule(rule)
                .build();
    }

    private LabelRule rule(String keyword) {
        return new LabelRule(List.of(new LabelRule.Group(List.of(
                new LabelRule.Condition(SUBJECT, CONTAINS, keyword)
        ))));
    }
}
