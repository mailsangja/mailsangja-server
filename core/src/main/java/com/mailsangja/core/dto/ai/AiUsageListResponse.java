package com.mailsangja.core.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI 기능 주간 사용량 목록 응답")
public record AiUsageListResponse(
        @Schema(description = "AI 기능별 주간 사용량 목록")
        List<AiUsageItemResponse> usages
) {
    public static AiUsageListResponse of(List<AiUsageItemResponse> usages) {
        return new AiUsageListResponse(usages);
    }
}
