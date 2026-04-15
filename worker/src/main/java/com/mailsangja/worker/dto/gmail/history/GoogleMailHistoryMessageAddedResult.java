package com.mailsangja.worker.dto.gmail.history;

public record GoogleMailHistoryMessageAddedResult(
        String gmailMessageId,
        String gmailThreadId
) {
}
