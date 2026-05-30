package com.mailsangja.core.service.ai.label;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.config.properties.AiModelProperties;
import com.mailsangja.core.config.properties.LabelSuggestionProperties;
import com.mailsangja.core.dto.label.LlmLabelSuggestionResult;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.MessageRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelSuggestionAiServiceTest {

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private ObjectProvider<ChatModel> chatModelProvider;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LabelSuggestionProperties properties;

    @Mock
    private AiModelProperties modelProperties;

    @Mock
    private SnippetPreprocessor snippetPreprocessor;

    @InjectMocks
    private LabelSuggestionAiService service;

    @Test
    void 최근수신메일없으면LLM호출없이빈결과반환() {
        UUID userId = UUID.randomUUID();
        when(properties.getRecentMailCount()).thenReturn(20);
        when(messageRepositoryPort.findRecentByUserIdAndDirection(
                eq(userId), eq(Direction.INBOUND), any(PageRequest.class)))
                .thenReturn(List.of());

        LlmLabelSuggestionResult result = service.suggest(userId, List.of());

        assertTrue(result.suggestions().isEmpty());
        verifyNoInteractions(chatModelProvider);
    }

    @Test
    void 최근수신메일없으면INBOUND방향으로조회한다() {
        UUID userId = UUID.randomUUID();
        when(properties.getRecentMailCount()).thenReturn(20);
        when(messageRepositoryPort.findRecentByUserIdAndDirection(
                eq(userId), eq(Direction.INBOUND), any(PageRequest.class)))
                .thenReturn(List.of());

        service.suggest(userId, List.of());

        verify(messageRepositoryPort).findRecentByUserIdAndDirection(
                eq(userId), eq(Direction.INBOUND), eq(PageRequest.of(0, 20)));
    }

    @Test
    void ChatModel미설정시LABEL_SUGGESTION_AI_FAILED예외발생() {
        UUID userId = UUID.randomUUID();
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .gmailMessageId("gmail-001")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
        when(properties.getRecentMailCount()).thenReturn(20);
        when(messageRepositoryPort.findRecentByUserIdAndDirection(
                eq(userId), eq(Direction.INBOUND), any(PageRequest.class)))
                .thenReturn(List.of(message));
        when(chatModelProvider.getIfAvailable()).thenReturn(null);

        LabelException exception = assertThrows(LabelException.class,
                () -> service.suggest(userId, List.of()));

        assertEquals(LabelErrorCode.LABEL_SUGGESTION_AI_FAILED, exception.getErrorCode());
    }

    @Test
    void 라벨추천모델을Prompt옵션에설정한다() throws Exception {
        UUID userId = UUID.randomUUID();
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .gmailMessageId("gmail-001")
                .direction(Direction.INBOUND)
                .subject("회의 일정")
                .fromAddress("sender@example.com")
                .read(false)
                .build();
        ChatModel chatModel = mock(ChatModel.class);
        when(properties.getRecentMailCount()).thenReturn(20);
        when(messageRepositoryPort.findRecentByUserIdAndDirection(
                eq(userId), eq(Direction.INBOUND), any(PageRequest.class)))
                .thenReturn(List.of(message));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(modelProperties.labelSuggestionModel()).thenReturn("label-test");
        when(chatModel.getDefaultOptions()).thenReturn(ChatOptions.builder().model("gpt-test").build());
        when(chatModel.call(any(Prompt.class))).thenReturn(response("{\"suggestions\":[]}"));

        service.suggest(userId, List.of());

        org.mockito.ArgumentCaptor<Prompt> captor = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        assertEquals("label-test", captor.getValue().getOptions().getModel());
    }

    private ChatResponse response(String text) {
        Generation generation = new Generation(AssistantMessage.builder().content(text).build());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().model("gpt-test").build();
        return new ChatResponse(List.of(generation), metadata);
    }
}
