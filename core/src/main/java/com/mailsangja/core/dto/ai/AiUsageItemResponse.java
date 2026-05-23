package com.mailsangja.core.dto.ai;

public record AiUsageItemResponse(
        AiUsageType type,
        long used,
        long limit
) {
}
