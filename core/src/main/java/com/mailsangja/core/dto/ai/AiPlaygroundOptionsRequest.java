package com.mailsangja.core.dto.ai;

import java.util.List;
import java.util.Map;

public record AiPlaygroundOptionsRequest(
        Double temperature,
        Integer maxOutputTokens,
        Integer maxCompletionTokens,
        Double topP,
        Double presencePenalty,
        Double frequencyPenalty,
        String responseFormat,
        String jsonSchema,
        Integer seed,
        List<String> stop,
        String reasoningEffort,
        String verbosity,
        String serviceTier,
        Map<String, Object> extraBody
) {

    public AiPlaygroundOptionsRequest {
        stop = stop == null ? List.of() : List.copyOf(stop);
        extraBody = extraBody == null ? Map.of() : Map.copyOf(extraBody);
    }
}
