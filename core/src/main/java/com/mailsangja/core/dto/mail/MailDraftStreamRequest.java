package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

import java.util.List;
import java.util.UUID;

public record MailDraftStreamRequest(
        String mailAddress,
        String query,
        UUID replyMessageId,
        List<String> to,
        List<String> cc,
        String model
) {

    public MailDraftStreamRequest {
        validateMailAddress(mailAddress);
        validateQuery(query);
    }

    public MailDraftStreamRequest(String mailAddress, String query, UUID replyMessageId, List<String> to, List<String> cc) {
        this(mailAddress, query, replyMessageId, to, cc, null);
    }

    private static void validateMailAddress(String mailAddress) {
        if (mailAddress == null || mailAddress.isBlank()) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

}
