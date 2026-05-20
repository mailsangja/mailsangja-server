package com.mailsangja.core.dto.ai;

import java.util.List;

public record AiModelListResult(
        String defaultModel,
        List<String> models
) {

    public AiModelListResult {
        models = models == null ? List.of() : List.copyOf(models);
    }

    public static AiModelListResult of(String defaultModel, List<String> models) {
        return new AiModelListResult(defaultModel, models);
    }
}
