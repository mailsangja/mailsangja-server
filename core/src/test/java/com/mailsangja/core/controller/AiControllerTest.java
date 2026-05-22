package com.mailsangja.core.controller;

import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.core.dto.ai.AiModelResponse;
import com.mailsangja.core.facade.AiFacade;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiControllerTest {

    @Test
    void getModels_ResponseEntity로모델목록을반환한다() {
        // given
        AiFacade aiFacade = mock(AiFacade.class);
        AiController controller = new AiController(aiFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        AiModelListResponse expected = new AiModelListResponse(
                "google/gemini-3.5-flash",
                List.of(new AiModelResponse("google/gemini-3.5-flash", true))
        );
        when(aiFacade.getModels()).thenReturn(expected);

        // when
        ResponseEntity<AiModelListResponse> response = controller.getModels(user);

        // then
        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
        verify(aiFacade).getModels();
    }
}
