package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.ai.AiPlaygroundErrorCode;
import com.mailsangja.core.common.exception.ai.AiPlaygroundException;
import com.mailsangja.core.dto.ai.AiModelListResult;
import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.core.dto.ai.AiPlaygroundChatRequest;
import com.mailsangja.core.dto.ai.AiPlaygroundChatResponse;
import com.mailsangja.core.dto.ai.AiPlaygroundChatResult;
import com.mailsangja.core.dto.ai.AiPlaygroundUsageResult;
import com.mailsangja.core.service.ai.AiPlaygroundCommandService;
import com.mailsangja.core.service.ai.AiQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiFacadeTest {

    @Test
    void getModels_ResponseDto로조립한다() {
        // given
        AiQueryService queryService = mock(AiQueryService.class);
        AiFacade facade = new AiFacade(
                mock(AiPlaygroundCommandService.class),
                queryService,
                mock(MailAccountQueryService.class)
        );
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

    @Test
    void chat_활성메일계정이있으면_Playground응답을ResponseDto로조립한다() {
        // given
        User user = createAdminUser();
        AiPlaygroundChatRequest request = createChatRequest();
        AiPlaygroundCommandService playgroundCommandService = mock(AiPlaygroundCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        AiFacade facade = new AiFacade(playgroundCommandService, mock(AiQueryService.class), mailAccountQueryService);

        when(mailAccountQueryService.findAllActiveByUserId(user.getId()))
                .thenReturn(List.of(mock(MailAccount.class)));
        when(playgroundCommandService.chat(request))
                .thenReturn(new AiPlaygroundChatResult(
                        "OPENROUTER",
                        "google/gemini-3.5-flash",
                        "content",
                        new AiPlaygroundUsageResult(10, 20, 30),
                        Map.of("model", "google/gemini-3.5-flash")
                ));

        // when
        AiPlaygroundChatResponse response = facade.chat(user, request);

        // then
        assertEquals("OPENROUTER", response.provider());
        assertEquals("google/gemini-3.5-flash", response.model());
        assertEquals("content", response.content());
        assertEquals(10, response.usage().inputTokens());
        assertEquals(20, response.usage().outputTokens());
        assertEquals(30, response.usage().totalTokens());
        verify(playgroundCommandService).chat(request);
    }

    @Test
    void chat_활성메일계정이없으면_예외를던지고_LLM을호출하지않는다() {
        // given
        User user = createAdminUser();
        AiPlaygroundChatRequest request = createChatRequest();
        AiPlaygroundCommandService playgroundCommandService = mock(AiPlaygroundCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        AiFacade facade = new AiFacade(playgroundCommandService, mock(AiQueryService.class), mailAccountQueryService);

        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of());

        // when
        AiPlaygroundException exception = assertThrows(
                AiPlaygroundException.class,
                () -> facade.chat(user, request)
        );

        // then
        assertEquals(AiPlaygroundErrorCode.MAIL_ACCOUNT_REQUIRED, exception.getErrorCode());
        verify(playgroundCommandService, never()).chat(request);
    }

    @Test
    void chat_관리자가아니면_예외를던지고_메일계정과LLM을조회하지않는다() {
        // given
        User user = createUser();
        AiPlaygroundChatRequest request = createChatRequest();
        AiPlaygroundCommandService playgroundCommandService = mock(AiPlaygroundCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        AiFacade facade = new AiFacade(playgroundCommandService, mock(AiQueryService.class), mailAccountQueryService);

        // when
        AiPlaygroundException exception = assertThrows(
                AiPlaygroundException.class,
                () -> facade.chat(user, request)
        );

        // then
        assertEquals(AiPlaygroundErrorCode.FORBIDDEN_USER, exception.getErrorCode());
        verify(mailAccountQueryService, never()).findAllActiveByUserId(user.getId());
        verify(playgroundCommandService, never()).chat(request);
    }

    private AiPlaygroundChatRequest createChatRequest() {
        return new AiPlaygroundChatRequest(
                "OPENROUTER",
                "google/gemini-3.5-flash",
                null,
                "메일을 작성해줘.",
                List.of(),
                Map.of(),
                null,
                Map.of()
        );
    }

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("테스트 사용자")
                .username("tester@example.com")
                .password("encoded")
                .role(Role.USER)
                .build();
    }

    private User createAdminUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("관리자")
                .username("admin@example.com")
                .password("encoded")
                .role(Role.ADMIN)
                .build();
    }
}
