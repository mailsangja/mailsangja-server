package com.mailsangja.core.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.config.properties.LabelSuggestionProperties;
import com.mailsangja.core.dto.label.LabelCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelSuggestionLoader implements ResourceLoaderAware {

    private final ObjectMapper objectMapper;
    private final LabelSuggestionProperties labelSuggestionProperties;

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public List<LabelCreateRequest> load() {
        String path = labelSuggestionProperties.getJsonPath();
        Resource resource = resourceLoader.getResource(path);
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Failed to load label suggestion JSON from path={}", path, e);
            throw new LabelException(LabelErrorCode.LABEL_SUGGESTION_LOAD_FAILED);
        }
    }
}
