package com.mailsangja.core.dto.label;

import java.util.Set;
import java.util.UUID;

public record LabelReclassifyMessage(
        UUID userId,
        Set<UUID> labelIds
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
        labelIds = Set.copyOf(labelIds);
    }
}
