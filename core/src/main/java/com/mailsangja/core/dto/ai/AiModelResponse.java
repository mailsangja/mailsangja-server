package com.mailsangja.core.dto.ai;

public record AiModelResponse(
        String id,
        boolean defaultModel
) {

    public static AiModelResponse of(String id, String defaultModel) {
        return new AiModelResponse(id, id != null && id.equals(defaultModel));
    }
}
