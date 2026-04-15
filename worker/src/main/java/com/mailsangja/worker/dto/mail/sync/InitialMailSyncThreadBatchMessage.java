package com.mailsangja.worker.dto.mail.sync;

import java.util.List;
import java.util.UUID;

public record InitialMailSyncThreadBatchMessage(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress,
        List<String> threadIds
) {
}
