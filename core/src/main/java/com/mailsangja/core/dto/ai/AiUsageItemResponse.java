package com.mailsangja.core.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 기능 주간 사용량 항목")
public record AiUsageItemResponse(
        @Schema(description = "AI 기능 타입")
        AiUsageType type,

        @Schema(description = "이번 주 사용 횟수", example = "3")
        long used,

        @Schema(description = "이번 주 사용 한도", example = "10")
        long limit
) {
}
