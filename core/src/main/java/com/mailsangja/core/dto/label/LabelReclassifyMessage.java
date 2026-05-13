package com.mailsangja.core.dto.label;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record LabelReclassifyMessage(
        UUID userId,
        Set<UUID> labelIds,
        List<UUID> threadIds,
        String jobId
) {

    public LabelReclassifyMessage {
        if (userId == null) {
            throw new LabelException(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST);
        }
        if (labelIds == null || labelIds.isEmpty()) {
            throw new LabelException(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST);
        }
        if (labelIds.stream().anyMatch(id -> id == null)) {
            throw new LabelException(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST);
        }
        if (threadIds == null || threadIds.isEmpty()) {
            throw new LabelException(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST);
        }
        if (threadIds.stream().anyMatch(id -> id == null)) {
            throw new LabelException(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST);
        }
        labelIds = Set.copyOf(labelIds);
        threadIds = List.copyOf(threadIds);
    }
}
