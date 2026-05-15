package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

public record MailDraftUsageEvent(String model, int inputTokens, int outputTokens, int totalTokens) {

    public MailDraftUsageEvent {
        validateToken(inputTokens);
        validateToken(outputTokens);
        validateToken(totalTokens);
    }

    public static MailDraftUsageEvent of(MailDraftUsageResult subject, MailDraftUsageResult body) {
        validateUsage(subject);
        validateUsage(body);
        return new MailDraftUsageEvent(
                modelOf(subject, body),
                subject.inputTokens() + body.inputTokens(),
                subject.outputTokens() + body.outputTokens(),
                subject.totalTokens() + body.totalTokens()
        );
    }

    private static String modelOf(MailDraftUsageResult subject, MailDraftUsageResult body) {
        if (body.model() != null) {
            return body.model();
        }
        return subject.model();
    }

    private static void validateUsage(MailDraftUsageResult usage) {
        if (usage == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateToken(int token) {
        if (token < 0) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }
}
