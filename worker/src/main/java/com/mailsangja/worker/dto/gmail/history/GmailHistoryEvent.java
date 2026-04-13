package com.mailsangja.worker.dto.gmail.history;

import java.util.UUID;

public record GmailHistoryEvent(
        GmailHistoryEventType eventType,
        UUID mailAccountId,
        String gmailMessageId,
        String gmailThreadId,
        String historyId
) {
}
