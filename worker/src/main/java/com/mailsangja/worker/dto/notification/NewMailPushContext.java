package com.mailsangja.worker.dto.notification;

import com.mailsangja.db.entity.mail.Direction;

import java.util.List;
import java.util.UUID;

public record NewMailPushContext(
        UUID mailAccountId,
        String alias,
        String subject,
        String snippet,
        UUID threadId,
        UUID messageId,
        Direction direction,
        List<String> toAddresses,
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
        this(mailAccountId, alias, subject, snippet, threadId, messageId, direction, List.of(), 0);
    }
}
