package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.dto.label.LabelGroupCreateRequest;
import com.mailsangja.core.dto.label.LabelGroupResponse;
import com.mailsangja.core.dto.label.LabelGroupUpdateRequest;
import com.mailsangja.core.service.label.LabelGroupCommandService;
import com.mailsangja.core.service.label.LabelGroupQueryService;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.label.LabelGroup;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LabelGroupFacade {

    private final LabelGroupQueryService labelGroupQueryService;
    private final LabelGroupCommandService labelGroupCommandService;

    public List<LabelGroupResponse> getLabelGroups(User user) {
        return labelGroupQueryService.findAllActiveByUserId(user.getId())
                .stream()
                .map(labelGroup -> LabelGroupResponse.of(labelGroup, labelGroup.getActiveLabels()))
                .toList();
    }

    public LabelGroupResponse getLabelGroupDetail(User user, UUID labelGroupId) {
        validateLabelGroupId(labelGroupId);
        LabelGroup labelGroup = labelGroupQueryService.findActiveByIdAndUserId(labelGroupId, user.getId());
        return LabelGroupResponse.of(labelGroup, labelGroup.getActiveLabels());
    }

    public LabelGroupResponse createLabelGroup(User user, LabelGroupCreateRequest request) {
        validateNameDuplicate(user.getId(), request.name());
        List<Label> labels = labelGroupQueryService.findOwnedActiveLabels(user.getId(), request.labelIds());
        LabelGroup labelGroup;
        try {
            labelGroup = labelGroupCommandService.create(user, request.name(), request.order(), labels);
        } catch (DataIntegrityViolationException e) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_NAME_DUPLICATE);
        }
        return LabelGroupResponse.of(labelGroup, labelGroup.getActiveLabels());
    }

    public LabelGroupResponse updateLabelGroup(User user, UUID labelGroupId, LabelGroupUpdateRequest request) {
        validateLabelGroupId(labelGroupId);
        LabelGroup labelGroup = labelGroupQueryService.findActiveByIdAndUserId(labelGroupId, user.getId());
        validateUpdateRequest(user, labelGroup, request);
        List<Label> labels = request.labelIds() != null
                ? labelGroupQueryService.findOwnedActiveLabels(user.getId(), request.labelIds())
                : null;
        LabelGroup updated;
        try {
            updated = labelGroupCommandService.update(labelGroup, request.name(), request.order(), labels);
        } catch (DataIntegrityViolationException e) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_NAME_DUPLICATE);
        }
        return LabelGroupResponse.of(updated, updated.getActiveLabels());
    }

    public void deleteLabelGroup(User user, UUID labelGroupId) {
        validateLabelGroupId(labelGroupId);
        LabelGroup labelGroup = labelGroupQueryService.findActiveByIdAndUserId(labelGroupId, user.getId());
        labelGroupCommandService.delete(labelGroup);
    }

    private void validateLabelGroupId(UUID labelGroupId) {
        if (labelGroupId == null) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_NOT_FOUND);
        }
    }

    private void validateUpdateRequest(User user, LabelGroup labelGroup, LabelGroupUpdateRequest request) {
        if (request.name() != null
                && labelGroupQueryService.existsByUserIdAndNameExcludingId(user.getId(), request.name().trim(), labelGroup.getId())) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_NAME_DUPLICATE);
        }
    }

    private void validateNameDuplicate(UUID userId, String name) {
        if (labelGroupQueryService.existsByUserIdAndName(userId, name.trim())) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_NAME_DUPLICATE);
        }
    }
}
