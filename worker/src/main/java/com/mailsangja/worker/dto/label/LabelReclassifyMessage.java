package com.mailsangja.worker.dto.label;

import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;

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
            throw new MqException(MqErrorCode.INVALID_LABEL_RECLASSIFY_MESSAGE);
        }
        if (labelIds == null || labelIds.isEmpty()) {
            throw new MqException(MqErrorCode.INVALID_LABEL_RECLASSIFY_MESSAGE);
        }
        if (labelIds.stream().anyMatch(id -> id == null)) {
            throw new MqException(MqErrorCode.INVALID_LABEL_RECLASSIFY_MESSAGE);
        }
        if (threadIds == null || threadIds.isEmpty()) {
            throw new MqException(MqErrorCode.INVALID_LABEL_RECLASSIFY_MESSAGE);
        }
        if (threadIds.stream().anyMatch(id -> id == null)) {
            throw new MqException(MqErrorCode.INVALID_LABEL_RECLASSIFY_MESSAGE);
        }
        labelIds = Set.copyOf(labelIds);
        threadIds = List.copyOf(threadIds);
    }
}
