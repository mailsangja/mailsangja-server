package com.mailsangja.core.service.label;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.label.LabelGroup;
import com.mailsangja.db.port.LabelGroupRepositoryPort;
import com.mailsangja.db.port.LabelRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabelGroupQueryService {

    private final LabelGroupRepositoryPort labelGroupRepositoryPort;
    private final LabelRepositoryPort labelRepositoryPort;

    public List<LabelGroup> findAllActiveByUserId(UUID userId) {
        return labelGroupRepositoryPort.findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(userId);
    }

    public LabelGroup findActiveByIdAndUserId(UUID labelGroupId, UUID userId) {
        return labelGroupRepositoryPort.findByIdAndUserIdAndDeletedAtIsNull(labelGroupId, userId)
                .orElseThrow(() -> new LabelException(LabelErrorCode.LABEL_GROUP_NOT_FOUND));
    }

    public List<Label> findOwnedActiveLabels(UUID userId, List<UUID> labelIds) {
        Set<UUID> uniqueLabelIds = new LinkedHashSet<>(labelIds);
        List<Label> labels = labelRepositoryPort.findAllByUserIdAndIdInAndDeletedAtIsNull(userId, List.copyOf(uniqueLabelIds));
        validateAllLabelsExist(labels, uniqueLabelIds);
        return labels;
    }

    public boolean existsByUserIdAndName(UUID userId, String name) {
        return labelGroupRepositoryPort.existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name);
    }

    public boolean existsByUserIdAndNameExcludingId(UUID userId, String name, UUID excludeId) {
        return labelGroupRepositoryPort.existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(userId, name, excludeId);
    }

    private void validateAllLabelsExist(List<Label> labels, Set<UUID> uniqueLabelIds) {
        if (labels.size() != uniqueLabelIds.size()) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_LABEL_NOT_FOUND);
        }
    }
}
