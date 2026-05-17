package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.common.util.LabelRuleValidator;
import com.mailsangja.core.common.util.LabelSuggestionLoader;
import com.mailsangja.core.dto.label.LabelCreateRequest;
import com.mailsangja.core.dto.label.LabelDetailResponse;
import com.mailsangja.core.dto.label.LabelListResponse;
import com.mailsangja.core.dto.label.LabelRuleUpdateRequest;
import com.mailsangja.core.dto.label.LabelUpdateRequest;
import com.mailsangja.core.service.label.LabelCommandService;
import com.mailsangja.core.service.label.LabelQueryService;
import com.mailsangja.core.service.label.LabelReclassifyPendingJobStore;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LabelFacade {

    private final LabelQueryService labelQueryService;
    private final LabelCommandService labelCommandService;
    private final LabelReclassifyPendingJobStore pendingJobStore;
    private final LabelRuleValidator labelRuleValidator;
    private final LabelSuggestionLoader labelSuggestionLoader;

    public List<LabelListResponse> getLabels(User user) {
        List<Label> labels = labelQueryService.findAllActiveByUserId(user.getId());
        Map<UUID, Long> unreadCounts = labelQueryService.findUnreadThreadCountsByUserId(user.getId());
        return labels.stream()
                .map(label -> LabelListResponse.of(label, unreadCounts.getOrDefault(label.getId(), 0L)))
                .toList();
    }

    public LabelDetailResponse getLabelDetail(User user, UUID labelId) {
        Label label = labelQueryService.findActiveByIdAndUserId(labelId, user.getId());
        return LabelDetailResponse.of(label, label.getRule());
    }

    public LabelDetailResponse createLabel(User user, LabelCreateRequest request) {
        validateCreateRequest(user, request);
        Label label;
        try {
            label = labelCommandService.create(user, request);
        } catch (DataIntegrityViolationException e) {
            throw new LabelException(LabelErrorCode.LABEL_NAME_DUPLICATE);
        }
        if (request.rule() != null) {
            pendingJobStore.checkRateLimit(user.getId());
            pendingJobStore.mergePendingJob(user.getId(), label.getId());
        }
        return LabelDetailResponse.of(label, label.getRule());
    }

    public LabelDetailResponse updateLabel(User user, UUID labelId, LabelUpdateRequest request) {
        Label label = labelQueryService.findActiveByIdAndUserId(labelId, user.getId());
        validateUpdateRequest(user, label, request);
        Label updated;
        try {
            updated = labelCommandService.update(label, request);
        } catch (DataIntegrityViolationException e) {
            throw new LabelException(LabelErrorCode.LABEL_NAME_DUPLICATE);
        }
        return LabelDetailResponse.of(updated, updated.getRule());
    }

    public LabelDetailResponse updateLabelRule(User user, UUID labelId, LabelRuleUpdateRequest request) {
        validateRuleRequest(request);
        Label label = labelQueryService.findActiveByIdAndUserId(labelId, user.getId());
        Label updated = labelCommandService.updateRule(label, request.rule());
        pendingJobStore.checkRateLimit(user.getId());
        pendingJobStore.mergePendingJob(user.getId(), updated.getId());
        return LabelDetailResponse.of(updated, updated.getRule());
    }

    public void deleteLabel(User user, UUID labelId) {
        Label label = labelQueryService.findActiveByIdAndUserId(labelId, user.getId());
        labelCommandService.delete(label);
    }

    public List<LabelListResponse> createSuggestions(User user) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<LabelCreateRequest> suggestions = labelSuggestionLoader.load();
        return suggestions.stream()
                .filter(request -> !labelQueryService.existsSuggestionByUserIdAndName(user.getId(), request.name().trim()))
                .map(request -> {
                    Label suggestion = labelCommandService.createSuggestion(user, request);
                    return LabelListResponse.of(suggestion, 0L);
                })
                .toList();
    }

    public LabelDetailResponse getSuggestionDetail(User user, UUID suggestionId) {
        Label suggestion = labelQueryService.findSuggestionByIdAndUserId(suggestionId, user.getId());
        return LabelDetailResponse.of(suggestion, suggestion.getRule());
    }

    public List<LabelListResponse> getSuggestions(User user) {
        return labelQueryService.findAllSuggestionsByUserId(user.getId()).stream()
                .map(label -> LabelListResponse.of(label, 0L))
                .toList();
    }

    public LabelDetailResponse approveSuggestion(User user, UUID suggestionId, LabelCreateRequest request) {
        validateApprovalRequest(user, suggestionId, request);
        Label suggestion = labelQueryService.findSuggestionByIdAndUserId(suggestionId, user.getId());
        Label approved;
        try {
            approved = labelCommandService.approveSuggestion(suggestion, request);
        } catch (DataIntegrityViolationException e) {
            throw new LabelException(LabelErrorCode.LABEL_NAME_DUPLICATE);
        }
        if (approved.getRule() != null) {
            pendingJobStore.checkRateLimit(user.getId());
            pendingJobStore.mergePendingJob(user.getId(), approved.getId());
        }
        return LabelDetailResponse.of(approved, approved.getRule());
    }

    public void deleteSuggestion(User user, UUID suggestionId) {
        Label suggestion = labelQueryService.findSuggestionByIdAndUserId(suggestionId, user.getId());
        labelCommandService.delete(suggestion);
    }

    private void validateApprovalRequest(User user, UUID suggestionId, LabelCreateRequest request) {
        if (labelQueryService.existsByUserIdAndNameExcludingId(user.getId(), request.name().trim(), suggestionId)) {
            throw new LabelException(LabelErrorCode.LABEL_NAME_DUPLICATE);
        }
        labelRuleValidator.validate(request.rule());
    }

    private void validateCreateRequest(User user, LabelCreateRequest request) {
        if (labelQueryService.existsByUserIdAndName(user.getId(), request.name().trim())) {
            throw new LabelException(LabelErrorCode.LABEL_NAME_DUPLICATE);
        }
        labelRuleValidator.validate(request.rule());
    }

    private void validateUpdateRequest(User user, Label label, LabelUpdateRequest request) {
        if (request.name() != null) {
            validateNameNotBlank(request.name());
            if (labelQueryService.existsByUserIdAndNameExcludingId(user.getId(), request.name().trim(), label.getId())) {
                throw new LabelException(LabelErrorCode.LABEL_NAME_DUPLICATE);
            }
        }
        if (request.colorCode() != null) {
            validateColorCode(request.colorCode());
        }
    }

    private void validateRuleRequest(LabelRuleUpdateRequest request) {
        labelRuleValidator.validate(request.rule());
    }

    private void validateNameNotBlank(String name) {
        if (name == null || name.isBlank()) {
            throw new LabelException(LabelErrorCode.LABEL_NAME_BLANK);
        }
    }

    private void validateColorCode(String colorCode) {
        if (colorCode == null || colorCode.isBlank()) {
            throw new LabelException(LabelErrorCode.LABEL_COLOR_CODE_BLANK);
        }
    }
}
