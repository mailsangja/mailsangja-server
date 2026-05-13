package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

public record MailDraftPromptResult(String systemPrompt, String userPrompt) {

    public MailDraftPromptResult {
        validateText(systemPrompt);
        validateText(userPrompt);
    }

    private static void validateText(String value) {
        if (value == null || value.isBlank()) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }
}
