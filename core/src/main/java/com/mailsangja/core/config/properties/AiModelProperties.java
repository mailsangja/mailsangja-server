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

    private String defaultModel;
    private String draftModel;
    private String labelSuggestionModel;
    private List<String> allowedModels = List.of();

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

    public String resolveDraft(String requestedModel) {
        String model = normalize(requestedModel);
        if (model == null) {
            model = normalize(draftModel);
        }
        return resolveDefaulted(model);
    }

    public String labelSuggestionModel() {
        return resolveDefaulted(normalize(labelSuggestionModel));
    }

    public List<String> allowedModels() {
        return normalizedAllowedModels();
    }

    private String resolveDefaulted(String model) {
        if (model == null) {
            model = normalize(defaultModel);
        }
        if (model == null || !normalizedAllowedModels().contains(model)) {
            throw new AiModelException(AiModelErrorCode.INVALID_MODEL);
        }
        return model;
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
