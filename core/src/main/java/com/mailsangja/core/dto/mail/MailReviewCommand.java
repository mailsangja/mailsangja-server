package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailReviewErrorCode;
import com.mailsangja.core.common.exception.mail.MailReviewException;

import java.util.List;
import java.util.UUID;

public record MailReviewCommand(
        UUID userId,
        String subject,
        String body,
        int attachmentCount,
        List<String> attachmentNames
) {

    public MailReviewCommand {
        if (userId == null || subject == null || body == null || (subject.isBlank() && body.isBlank())) {
            throw new MailReviewException(MailReviewErrorCode.INVALID_REQUEST);
        }
        if (attachmentCount < 0 || attachmentNames == null || attachmentNames.stream().anyMatch(name -> name == null || name.isBlank())) {
            throw new MailReviewException(MailReviewErrorCode.INVALID_REQUEST);
        }
        attachmentNames = List.copyOf(attachmentNames);
    }

    public static MailReviewCommand of(UUID userId, MailReviewRequest request) {
        return new MailReviewCommand(userId, request.subject(), request.body(), request.attachmentCount(), request.attachmentNames());
    }

    public boolean hasAttachments() {
        return attachmentCount > 0;
    }
}
