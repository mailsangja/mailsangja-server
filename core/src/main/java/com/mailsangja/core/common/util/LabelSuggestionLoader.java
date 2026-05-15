package com.mailsangja.core.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.config.properties.LabelSuggestionProperties;
import com.mailsangja.core.dto.label.LabelCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelSuggestionLoader {

    private final ObjectMapper objectMapper;
    private final LabelSuggestionProperties labelSuggestionProperties;

    public List<LabelCreateRequest> load() {
        String json = labelSuggestionProperties.getJson();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to load label suggestions from LABEL_SUGGESTIONS_JSON env", e);
            throw new LabelException(LabelErrorCode.LABEL_SUGGESTION_LOAD_FAILED);
        }
    }
}
