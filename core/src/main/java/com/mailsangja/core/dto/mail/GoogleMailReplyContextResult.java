package com.mailsangja.core.dto.mail;

public record GoogleMailReplyContextResult(
        String gmailThreadId,
        String parentRfcMessageId,
        String referencesHeader,
        String subject
) {
}
