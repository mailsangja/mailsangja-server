package com.mailsangja.core.dto.mail;

public record MailDraftUsageResult(
        String model,
        int inputTokens,
        int outputTokens,
        int totalTokens
) {
}
