package com.mailsangja.core.dto.ai;

import java.util.Map;

public record AiPlaygroundChatResponse(
        String provider,
        String model,
        String content,
        AiPlaygroundUsageResponse usage,
        Map<String, Object> raw
) {

    public AiPlaygroundChatResponse {
        raw = raw == null ? Map.of() : Map.copyOf(raw);
    }

    public static AiPlaygroundChatResponse from(AiPlaygroundChatResult result) {
        return new AiPlaygroundChatResponse(
                result.provider(),
                result.model(),
                result.content(),
                AiPlaygroundUsageResponse.from(result.usage()),
                result.raw()
        );
    }
}
