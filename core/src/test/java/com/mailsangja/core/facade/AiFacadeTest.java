package com.mailsangja.core.facade;

import com.mailsangja.core.dto.ai.AiModelListResult;
import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.core.service.ai.AiQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiFacadeTest {

    @Test
    void getModels_ResponseDto로조립한다() {
        // given
        AiQueryService queryService = mock(AiQueryService.class);
        AiFacade facade = new AiFacade(queryService);
        when(queryService.getModels()).thenReturn(AiModelListResult.of(
                "google/gemini-3.5-flash",
                List.of("google/gemini-3.5-flash", "openai/gpt-5.5")
        ));

        // when
        AiModelListResponse response = facade.getModels();

        // then
        assertEquals("google/gemini-3.5-flash", response.defaultModel());
        assertEquals(2, response.models().size());
        assertEquals("google/gemini-3.5-flash", response.models().getFirst().id());
        assertEquals(true, response.models().getFirst().defaultModel());
    }
}
