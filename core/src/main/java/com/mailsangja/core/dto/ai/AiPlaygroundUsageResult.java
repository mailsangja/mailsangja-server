package com.mailsangja.core.dto.ai;

public record AiPlaygroundUsageResult(
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens
) {
}
