package com.mailsangja.core.service.label;

import com.mailsangja.core.dto.label.LabelCreateRequest;
import com.mailsangja.core.dto.label.LabelUpdateRequest;
import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.common.label.NotificationPolicy;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.LabelGroupRepositoryPort;
import com.mailsangja.db.port.LabelRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static com.mailsangja.db.common.label.LabelRule.Field.SUBJECT;
import static com.mailsangja.db.common.label.LabelRule.Operator.CONTAINS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelCommandServiceTest {

    @Mock
    private LabelRepositoryPort labelRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private LabelGroupRepositoryPort labelGroupRepositoryPort;

    @InjectMocks
    private LabelCommandService labelCommandService;

    @Test
    void create_notificationPolicyIsNull_defaultsToInheritAndTrimsName() {
        User user = User.builder().id(UUID.randomUUID()).build();
        LabelRule rule = rule("invoice");
        LabelCreateRequest request = new LabelCreateRequest("  업무  ", "#3366FF", null, 3, true, rule);
        when(labelRepositoryPort.save(any(Label.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Label saved = labelCommandService.create(user, request);

        assertEquals(user, saved.getUser());
        assertEquals("업무", saved.getName());
        assertEquals("#3366FF", saved.getColorCode());
        assertEquals(NotificationPolicy.INHERIT, saved.getNotificationPolicy());
        assertEquals(3, saved.getDisplayOrder());
        assertTrue(saved.isSensitive());
        assertSame(rule, saved.getRule());
    }

    @Test
    void update_onlyNonNullFieldsAreChangedAndNameIsTrimmed() {
        Label label = Label.builder()
                .id(UUID.randomUUID())
                .name("기존")
                .colorCode("#111111")
                .notificationPolicy(NotificationPolicy.INHERIT)
                .displayOrder(1)
                .isSensitive(false)
                .build();
        LabelUpdateRequest request = new LabelUpdateRequest("  새 이름  ", null, NotificationPolicy.SILENT, null, true);
        when(labelRepositoryPort.save(label)).thenReturn(label);

        Label updated = labelCommandService.update(label, request);

        assertSame(label, updated);
        assertEquals("새 이름", label.getName());
        assertEquals("#111111", label.getColorCode());
        assertEquals(NotificationPolicy.SILENT, label.getNotificationPolicy());
        assertEquals(1, label.getDisplayOrder());
        assertTrue(label.isSensitive());
    }

    @Test
    void updateRule_replacesRule() {
        Label label = Label.builder()
                .id(UUID.randomUUID())
                .name("업무")
                .rule(rule("old"))
                .build();
        LabelRule newRule = rule("new");
        when(labelRepositoryPort.save(label)).thenReturn(label);

        Label updated = labelCommandService.updateRule(label, newRule);

        assertSame(label, updated);
        assertSame(newRule, label.getRule());
    }

    @Test
    void delete_removesMessageLabelsSoftDeletesLabelAndSaves() {
        UUID labelId = UUID.randomUUID();
        Label label = Label.builder()
                .id(labelId)
                .name("삭제 대상")
                .build();
        when(labelGroupRepositoryPort.findActiveGroupsByLabelId(labelId)).thenReturn(List.of());

        labelCommandService.delete(label);

        verify(messageRepositoryPort).deleteMessageLabelsByLabelId(labelId);
        verify(labelGroupRepositoryPort).findActiveGroupsByLabelId(labelId);
        verify(labelGroupRepositoryPort).saveAll(List.of());
        ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
        verify(labelRepositoryPort).save(labelCaptor.capture());
        assertSame(label, labelCaptor.getValue());
        assertTrue(label.isDeleted());
    }

    private LabelRule rule(String keyword) {
        return new LabelRule(List.of(new LabelRule.Group(List.of(
                new LabelRule.Condition(SUBJECT, CONTAINS, keyword)
        ))));
    }
}
