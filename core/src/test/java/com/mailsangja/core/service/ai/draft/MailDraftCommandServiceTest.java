package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageResult;
import com.mailsangja.db.port.MailDraftRateLimitCachePort;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailDraftCommandServiceTest {

    @Test
    void 월간50회까지는허용한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailDraftRateLimitCachePort cachePort = mock(MailDraftRateLimitCachePort.class);
        MailDraftCommandService service = new MailDraftCommandService(cachePort, chatModelProvider());
        when(cachePort.incrementMonthlyCount(userId)).thenReturn(50L);

        // when & then
        assertDoesNotThrow(() -> service.validateMonthlyRateLimit(userId));

        // then
        verify(cachePort).incrementMonthlyCount(userId);
    }

    @Test
    void 월간51번째요청은거부한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailDraftRateLimitCachePort cachePort = mock(MailDraftRateLimitCachePort.class);
        MailDraftCommandService service = new MailDraftCommandService(cachePort, chatModelProvider());
        when(cachePort.incrementMonthlyCount(userId)).thenReturn(51L);

        // when & then
        assertThrows(MailDraftException.class, () -> service.validateMonthlyRateLimit(userId));
    }

    @Test
    void subject는eventName과payloadPhase로구분한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        MailDraftCommandService service = createService();

        // when
        service.sendDelta(emitter, MailDraftPhase.SUBJECT, "제목");

        // then
        assertEquals("subject", emitter.eventName());
        assertEquals(MailDraftPhase.SUBJECT, emitter.payload().phase());
        assertEquals("제목", emitter.payload().delta());
    }

    @Test
    void body는eventName과payloadPhase로구분한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        MailDraftCommandService service = createService();

        // when
        service.sendDelta(emitter, MailDraftPhase.BODY, "본문");

        // then
        assertEquals("body", emitter.eventName());
        assertEquals(MailDraftPhase.BODY, emitter.payload().phase());
        assertEquals("본문", emitter.payload().delta());
    }

    @Test
    void 응답delta에대해서만토큰을복원한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        MailDraftCommandService service = createService();
        MailDraftRestoreContextResult restoreContext = new MailDraftRestoreContextResult(Map.of("[EMAIL_1]", "alice@example.com"));

        // when
        service.sendDelta(emitter, MailDraftPhase.BODY, "수신자 [EMAIL_1]", restoreContext);

        // then
        assertEquals("수신자 alice@example.com", emitter.payload().delta());
    }

    @Test
    void subject스트림은ChatModel응답을subject이벤트로전송하고사용량을반환한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("제목", "gpt-test", usage(10, 3)));
        MailDraftCommandService service = createService(chatModel);

        // when
        MailDraftUsageResult result = service.streamSubject(emitter, prompt());

        // then
        assertEquals("subject", emitter.eventName());
        assertEquals("제목", emitter.payload().delta());
        assertEquals(new MailDraftUsageResult("gpt-test", 10, 3, 13), result);
    }

    @Test
    void body스트림은ChatModel응답을body이벤트로전송하고사용량을반환한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("본문", "gpt-test", usage(20, 7)));
        MailDraftCommandService service = createService(chatModel);

        // when
        MailDraftUsageResult result = service.streamBody(emitter, prompt());

        // then
        assertEquals("body", emitter.eventName());
        assertEquals("본문", emitter.payload().delta());
        assertEquals(new MailDraftUsageResult("gpt-test", 20, 7, 27), result);
    }

    private MailDraftCommandService createService() {
        return new MailDraftCommandService(mock(MailDraftRateLimitCachePort.class), chatModelProvider());
    }

    private MailDraftCommandService createService(ChatModel chatModel) {
        return new MailDraftCommandService(mock(MailDraftRateLimitCachePort.class), chatModelProvider(chatModel));
    }

    private ObjectProvider<ChatModel> chatModelProvider() {
        return chatModelProvider(mock(ChatModel.class));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ChatModel> chatModelProvider(ChatModel chatModel) {
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(chatModel);
        return provider;
    }

    private ChatModel chatModel(ChatResponse response) {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(response));
        return chatModel;
    }

    private ChatResponse response(String text, String model, Usage usage) {
        Generation generation = new Generation(AssistantMessage.builder().content(text).build());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().model(model).usage(usage).build();
        return new ChatResponse(List.of(generation), metadata);
    }

    private Usage usage(int promptTokens, int completionTokens) {
        return new TestUsage(promptTokens, completionTokens);
    }

    private MailDraftPromptResult prompt() {
        return new MailDraftPromptResult("system", "user");
    }

    private static final class CapturingSseEmitter extends SseEmitter {

        private String eventName;
        private MailDraftDeltaEvent payload;

        @Override
        public synchronized void send(SseEventBuilder builder) {
            MailDraftCommandService.CapturedEvent event = MailDraftCommandService.capture(builder);
            this.eventName = event.name();
            this.payload = (MailDraftDeltaEvent) event.data();
        }

        private String eventName() {
            return eventName;
        }

        private MailDraftDeltaEvent payload() {
            return payload;
        }
    }

    private record TestUsage(Integer getPromptTokens, Integer getCompletionTokens) implements Usage {

        public Object getNativeUsage() {
            return null;
        }
    }
}
