package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailReviewErrorCode;
import com.mailsangja.core.common.exception.mail.MailReviewException;

public record MailReviewRequest(
        String subject,
        String body
) {

    private static final int MAX_SUBJECT_LENGTH = 500;
    private static final int MAX_BODY_LENGTH = 20_000;

    public MailReviewRequest {
        subject = nullToEmpty(subject);
        body = nullToEmpty(body);
        validateNotBlank(subject, body);
        validateLength(subject, MAX_SUBJECT_LENGTH);
        validateLength(body, MAX_BODY_LENGTH);
    }

    private static void validateNotBlank(String subject, String body) {
        if (subject.isBlank() && body.isBlank()) {
            throw new MailReviewException(MailReviewErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateLength(String value, int maxLength) {
        if (value.length() > maxLength) {
            throw new MailReviewException(MailReviewErrorCode.INVALID_REQUEST);
        }
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
