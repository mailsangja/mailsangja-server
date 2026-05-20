package com.mailsangja.core.dto.ai;

import java.util.List;
import java.util.Map;

public record AiPlaygroundChatRequest(
        String provider,
        String model,
        String systemPrompt,
        String userPrompt,
        List<AiPlaygroundMessageRequest> messages,
        Map<String, Object> context,
        AiPlaygroundOptionsRequest options,
        Map<String, Object> metadata
) {

    public AiPlaygroundChatRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        context = context == null ? Map.of() : Map.copyOf(context);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
