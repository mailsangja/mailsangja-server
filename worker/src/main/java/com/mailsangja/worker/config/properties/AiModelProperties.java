package com.mailsangja.worker.config.properties;

import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;
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
    private String replyDraftSuggestionModel;
    private List<String> allowedModels = List.of();

    public String replyDraftSuggestionModel() {
        String model = normalize(replyDraftSuggestionModel);
        if (model == null) {
            model = normalize(defaultModel);
        }
        if (model == null || !normalizedAllowedModels().contains(model)) {
            throw new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE);
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
