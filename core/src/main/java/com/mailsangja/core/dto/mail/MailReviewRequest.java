package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailReviewErrorCode;
import com.mailsangja.core.common.exception.mail.MailReviewException;

import java.util.List;

public record MailReviewRequest(
        String subject,
        String body,
        int attachmentCount,
        List<String> attachmentNames
) {

    private static final int MAX_SUBJECT_LENGTH = 500;
    private static final int MAX_BODY_LENGTH = 20_000;

    public MailReviewRequest {
        subject = nullToEmpty(subject);
        body = nullToEmpty(body);
        attachmentNames = normalizeAttachmentNames(attachmentNames);
        validateNotBlank(subject, body);
        validateLength(subject, MAX_SUBJECT_LENGTH);
        validateLength(body, MAX_BODY_LENGTH);
        validateAttachmentCount(attachmentCount);
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

    private static void validateAttachmentCount(int attachmentCount) {
        if (attachmentCount < 0) {
            throw new MailReviewException(MailReviewErrorCode.INVALID_REQUEST);
        }
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private static List<String> normalizeAttachmentNames(List<String> attachmentNames) {
        if (attachmentNames == null) {
            return List.of();
        }
        return attachmentNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::strip)
                .toList();
    }
}
