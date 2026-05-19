package com.mailsangja.core.service.ai.label;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.config.properties.LabelSuggestionProperties;
import com.mailsangja.core.dto.label.LlmLabelSuggestionResult;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.MessageRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}
