package com.mailsangja.core.dto.mail;

public record GoogleMailSendResponse(
        String id,
        String threadId,
        String historyId,
        String snippet,
        String internalDate
) {
}
