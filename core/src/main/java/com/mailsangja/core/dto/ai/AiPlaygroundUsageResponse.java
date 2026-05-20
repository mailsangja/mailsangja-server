package com.mailsangja.core.dto.ai;

public record AiPlaygroundUsageResponse(
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens
) {

    public static AiPlaygroundUsageResponse from(AiPlaygroundUsageResult result) {
        if (result == null) {
            return new AiPlaygroundUsageResponse(null, null, null);
        }
        return new AiPlaygroundUsageResponse(result.inputTokens(), result.outputTokens(), result.totalTokens());
    }
}
