package com.mailsangja.core.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.common.exception.ai.AiPlaygroundErrorCode;
import com.mailsangja.core.common.exception.ai.AiPlaygroundException;
import com.mailsangja.core.dto.ai.AiPlaygroundChatRequest;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.port.AiPlaygroundRateLimitCachePort;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPlaygroundCommandServiceTest {

    @Test
    void free사용자는한도초과시예외를던진다() {
        UUID userId = UUID.randomUUID();
        AiPlaygroundRateLimitCachePort cachePort = mock(AiPlaygroundRateLimitCachePort.class);
        when(cachePort.tryConsumeWeeklyLimit(userId)).thenReturn(false);
        AiPlaygroundCommandService service = new AiPlaygroundCommandService(
                cachePort,
                chatModelProvider(chatModel("응답")),
                new ObjectMapper()
        );

        AiPlaygroundException exception = assertThrows(
                AiPlaygroundException.class,
                () -> service.chat(userId, request(), Plan.FREE)
        );

        assertEquals(AiPlaygroundErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        verify(cachePort).tryConsumeWeeklyLimit(userId);
    }

    @Test
    void pro사용자는한도초과여도카운트만소모하고허용한다() {
        UUID userId = UUID.randomUUID();
        AiPlaygroundRateLimitCachePort cachePort = mock(AiPlaygroundRateLimitCachePort.class);
        when(cachePort.tryConsumeWeeklyLimit(userId)).thenReturn(false);
        AiPlaygroundCommandService service = new AiPlaygroundCommandService(
                cachePort,
                chatModelProvider(chatModel("응답")),
                new ObjectMapper()
        );

        assertDoesNotThrow(() -> service.chat(userId, request(), Plan.PRO));

        verify(cachePort).tryConsumeWeeklyLimit(userId);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ChatModel> chatModelProvider(ChatModel chatModel) {
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(chatModel);
        return provider;
    }

    private AiPlaygroundChatRequest request() {
        return new AiPlaygroundChatRequest(
                "OPENROUTER",
                "gpt-test",
                null,
                "메일을 작성해줘.",
                List.of(),
                Map.of(),
                null,
                Map.of()
        );
    }

    private ChatModel chatModel(String text) {
        return new StubChatModel(response(text));
    }

    private ChatResponse response(String text) {
        Generation generation = new Generation(AssistantMessage.builder().content(text).build());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().model("gpt-test").build();
        return new ChatResponse(List.of(generation), metadata);
    }

    private record StubChatModel(ChatResponse response) implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            return response;
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return ChatOptions.builder().model("gpt-test").build();
        }
    }
}
