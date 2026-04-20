package com.mailsangja.worker.dto.notification;

import java.util.UUID;

public record NewMailPushContext(
        UUID mailAccountId,
        String alias,
        String subject,
        String snippet,
        UUID threadId,
        UUID messageId
) {
}
