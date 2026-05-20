package com.mailsangja.core.service.ai;

import com.mailsangja.core.config.properties.AiModelProperties;
import com.mailsangja.core.dto.ai.AiModelListResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiQueryServiceTest {

    @Test
    void getModels_설정된기본모델과허용모델을반환한다() {
        // given
        AiModelProperties properties = new AiModelProperties();
        properties.setDefaultModel("google/gemini-3.5-flash");
        properties.setAllowedModels(List.of("google/gemini-3.5-flash", "openai/gpt-5.5"));
        AiQueryService service = new AiQueryService(properties);

        // when
        AiModelListResult result = service.getModels();

        // then
        assertEquals("google/gemini-3.5-flash", result.defaultModel());
        assertEquals(List.of("google/gemini-3.5-flash", "openai/gpt-5.5"), result.models());
    }
}
