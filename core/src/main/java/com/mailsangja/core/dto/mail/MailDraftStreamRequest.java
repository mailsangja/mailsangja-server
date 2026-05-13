package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

import java.util.List;
import java.util.UUID;

public record MailDraftStreamRequest(
        UUID mailAccountId,
        String query,
        UUID replyMessageId,
        List<String> to,
        List<String> cc
) {

    public MailDraftStreamRequest {
        validateMailAccountId(mailAccountId);
        validateQuery(query);
        validateTo(to);
    }

    private static void validateMailAccountId(UUID mailAccountId) {
        if (mailAccountId == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateTo(List<String> to) {
        if (to == null || to.isEmpty()) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }
}
