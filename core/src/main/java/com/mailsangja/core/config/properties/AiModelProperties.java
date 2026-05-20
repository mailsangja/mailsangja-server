package com.mailsangja.core.config.properties;

import com.mailsangja.core.common.exception.ai.AiModelErrorCode;
import com.mailsangja.core.common.exception.ai.AiModelException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "mailsangja.ai.model")
public class AiModelProperties {

    private String defaultModel = "google/gemini-3.5-flash";
    private List<String> allowedModels = List.of(
            "google/gemini-3.5-flash",
            "google/gemini-3.1-pro-preview",
            "google/gemini-3.1-flash-lite",
            "google/gemini-3-pro-preview",
            "google/gemini-3-flash-preview",
            "google/gemini-2.5-pro",
            "google/gemini-2.5-flash",
            "google/gemini-2.5-flash-lite",
            "openai/gpt-5.5",
            "openai/gpt-5.5-pro",
            "openai/gpt-5.4-nano",
            "openai/gpt-5.4-mini",
            "openai/gpt-5.4",
            "openai/gpt-5.4-pro",
            "openai/gpt-5.3-chat",
            "openai/gpt-5.3-codex",
            "openai/gpt-5.2-chat",
            "openai/gpt-5.2",
            "openai/gpt-5.1",
            "openai/gpt-4.1",
            "openai/gpt-4.1-mini",
            "openai/gpt-4.1-nano",
            "qwen/qwen3.6-flash",
            "qwen/qwen3.5-plus-20260420",
            "anthropic/claude-haiku-4.5",
            "anthropic/claude-sonnet-4.6",
            "anthropic/claude-sonnet-4.5",
            "anthropic/claude-sonnet-4",
            "anthropic/claude-opus-4.7",
            "anthropic/claude-opus-4.7-fast",
            "anthropic/claude-opus-4.6",
            "anthropic/claude-opus-4.6-fast",
            "anthropic/claude-opus-4.1",
            "anthropic/claude-opus-4",
            "mistralai/mistral-medium-3-5",
            "x-ai/grok-4.3"
    );

    public String resolve(String requestedModel) {
        String model = normalize(requestedModel);
        if (model == null) {
            model = normalize(defaultModel);
        }
        if (model == null || !normalizedAllowedModels().contains(model)) {
            throw new AiModelException(AiModelErrorCode.INVALID_MODEL);
        }
        return model;
    }

    public String defaultModel() {
        return resolve(null);
    }

    public List<String> allowedModels() {
        return normalizedAllowedModels();
    }

    private List<String> normalizedAllowedModels() {
        if (allowedModels == null) {
            return List.of();
        }
        return allowedModels.stream()
                .map(this::normalize)
                .filter(value -> value != null)
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
