package com.mailsangja.core.dto.ai;

import java.util.Map;

public record AiPlaygroundChatResult(
        String provider,
        String model,
        String content,
        AiPlaygroundUsageResult usage,
        Map<String, Object> raw
) {

    public AiPlaygroundChatResult {
        raw = raw == null ? Map.of() : Map.copyOf(raw);
    }
}
