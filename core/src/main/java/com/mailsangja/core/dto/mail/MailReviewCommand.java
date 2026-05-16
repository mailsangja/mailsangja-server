package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailReviewErrorCode;
import com.mailsangja.core.common.exception.mail.MailReviewException;

import java.util.UUID;

public record MailReviewCommand(
        UUID userId,
        String subject,
        String body
) {

    public MailReviewCommand {
        if (userId == null || subject == null || body == null || (subject.isBlank() && body.isBlank())) {
            throw new MailReviewException(MailReviewErrorCode.INVALID_REQUEST);
        }
    }

    public static MailReviewCommand of(UUID userId, MailReviewRequest request) {
        return new MailReviewCommand(userId, request.subject(), request.body());
    }
}
