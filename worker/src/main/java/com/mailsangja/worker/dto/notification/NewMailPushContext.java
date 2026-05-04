package com.mailsangja.worker.dto.notification;

import com.mailsangja.db.entity.mail.Direction;

import java.util.UUID;

public record NewMailPushContext(
        UUID mailAccountId,
        String alias,
        String subject,
        String snippet,
        UUID threadId,
        UUID messageId,
        Direction direction
) {
}
