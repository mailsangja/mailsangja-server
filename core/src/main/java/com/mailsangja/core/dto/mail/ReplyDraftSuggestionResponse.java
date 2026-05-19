package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;

import java.util.UUID;

public record ReplyDraftSuggestionResponse(
        UUID id,
        String type,
        String subject,
        String body
) {

    public ReplyDraftSuggestionResponse {
        if (id == null || isBlank(type) || isBlank(subject) || isBlank(body)) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    public static ReplyDraftSuggestionResponse from(ReplyDraftSuggestion suggestion) {
        if (suggestion == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
        return new ReplyDraftSuggestionResponse(
                suggestion.getId(),
                suggestion.getType(),
                suggestion.getSubject(),
                suggestion.getBody()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
