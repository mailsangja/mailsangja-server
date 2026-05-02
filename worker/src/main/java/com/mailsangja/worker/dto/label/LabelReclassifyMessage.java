package com.mailsangja.worker.dto.label;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record LabelReclassifyMessage(
        UUID userId,
        Set<UUID> labelIds,
        List<UUID> threadIds
) {

    public LabelReclassifyMessage {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (labelIds == null || labelIds.isEmpty()) {
            throw new IllegalArgumentException("labelIds must not be null or empty");
        }
        if (labelIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("labelIds must not contain null");
        }
        if (threadIds == null || threadIds.isEmpty()) {
            throw new IllegalArgumentException("threadIds must not be null or empty");
        }
        labelIds = Set.copyOf(labelIds);
        threadIds = List.copyOf(threadIds);
    }
}
