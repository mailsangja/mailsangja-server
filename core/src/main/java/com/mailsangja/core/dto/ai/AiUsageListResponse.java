package com.mailsangja.core.dto.ai;

import java.util.List;

public record AiUsageListResponse(
        List<AiUsageItemResponse> usages
) {
    public static AiUsageListResponse of(List<AiUsageItemResponse> usages) {
        return new AiUsageListResponse(usages);
    }
}
