package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

import java.util.UUID;

public record MailDraftSearchContextResult(
        UUID messageId,
        String source,
        String subject,
        String body
) {

    public MailDraftSearchContextResult {
        validateMessageId(messageId);
        source = nullToEmpty(source);
        subject = nullToEmpty(subject);
        body = nullToEmpty(body);
    }

    private static void validateMessageId(UUID messageId) {
        if (messageId == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
