package com.mailsangja.core.service.ai;

import com.mailsangja.core.config.properties.AiModelProperties;
import com.mailsangja.core.dto.ai.AiModelListResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiQueryService {

    private final AiModelProperties modelProperties;

    public AiModelListResult getModels() {
        return AiModelListResult.of(modelProperties.defaultModel(), modelProperties.allowedModels());
    }
}
