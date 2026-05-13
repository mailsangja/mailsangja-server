package com.mailsangja.core.dto.label;

import java.util.Set;
import java.util.UUID;

public record LabelReclassifyPendingJob(
        UUID userId,
        Set<UUID> labelIds
) {
}
