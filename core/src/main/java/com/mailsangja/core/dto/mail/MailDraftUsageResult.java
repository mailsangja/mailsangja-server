package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

public record MailDraftUsageResult(
        String model,
        int inputTokens,
        int outputTokens,
        int totalTokens
) {

    public MailDraftUsageResult {
        validateToken(inputTokens);
        validateToken(outputTokens);
        validateToken(totalTokens);
    }

    private static void validateToken(int token) {
        if (token < 0) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }
}
