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
        Direction direction,
        int threadMessageCount
) {
    public NewMailPushContext(
            UUID mailAccountId,
            String alias,
            String subject,
            String snippet,
            UUID threadId,
            UUID messageId,
            Direction direction
    ) {
        this(mailAccountId, alias, subject, snippet, threadId, messageId, direction, 0);
    }
}
