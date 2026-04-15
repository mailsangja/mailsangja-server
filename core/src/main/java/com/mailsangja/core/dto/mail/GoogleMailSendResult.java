package com.mailsangja.core.dto.mail;

public record GoogleMailSendResult(
        String gmailMessageId,
        String gmailThreadId
) {
}
