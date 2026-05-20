package com.mailsangja.core.dto.ai;

import java.util.List;

public record AiModelListResponse(
        String defaultModel,
        List<AiModelResponse> models
) {

    public AiModelListResponse {
        models = models == null ? List.of() : List.copyOf(models);
    }

    public static AiModelListResponse from(AiModelListResult result) {
        return new AiModelListResponse(
                result.defaultModel(),
                result.models().stream()
                        .map(model -> AiModelResponse.of(model, result.defaultModel()))
                        .toList()
        );
    }
}
