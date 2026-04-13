package com.mailsangja.worker.dto.mail.sync;

import java.util.UUID;

public record InitialMailSyncMessage(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress
) {
}
