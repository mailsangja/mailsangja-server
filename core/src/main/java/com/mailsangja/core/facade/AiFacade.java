package com.mailsangja.core.facade;

import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.core.service.ai.AiQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiFacade {

    private final AiQueryService aiQueryService;

    public AiModelListResponse getModels() {
        return AiModelListResponse.from(aiQueryService.getModels());
    }
}
