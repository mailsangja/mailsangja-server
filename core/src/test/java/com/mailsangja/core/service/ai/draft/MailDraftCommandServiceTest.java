package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.common.exception.ai.AiModelException;
import com.mailsangja.core.config.properties.AiModelProperties;
import com.mailsangja.core.dto.mail.MailDraftDeltaEvent;
import com.mailsangja.core.dto.mail.MailDraftDoneEvent;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageEvent;
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
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
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
        MailDraftCommandService service = new MailDraftCommandService(cachePort, chatModelProvider(), modelProperties());
        when(cachePort.tryConsumeMonthlyLimit(userId)).thenReturn(true);

        // when & then
        assertDoesNotThrow(() -> service.validateMonthlyRateLimit(userId));

        // then
        verify(cachePort).tryConsumeMonthlyLimit(userId);
    }

    @Test
    void 월간51번째요청은거부한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailDraftRateLimitCachePort cachePort = mock(MailDraftRateLimitCachePort.class);
        MailDraftCommandService service = new MailDraftCommandService(cachePort, chatModelProvider(), modelProperties());
        when(cachePort.tryConsumeMonthlyLimit(userId)).thenReturn(false);

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
        assertEquals(MailDraftPhase.SUBJECT, emitter.deltaPayload().phase());
        assertEquals("제목", emitter.deltaPayload().delta());
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
        assertEquals(MailDraftPhase.BODY, emitter.deltaPayload().phase());
        assertEquals("본문", emitter.deltaPayload().delta());
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
        assertEquals("수신자 alice@example.com", emitter.deltaPayload().delta());
    }

    @Test
    void 긴토큰을짧은토큰보다먼저복원한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        MailDraftCommandService service = createService();
        MailDraftRestoreContextResult restoreContext = new MailDraftRestoreContextResult(Map.of(
                "[EMAIL_1]", "one@example.com",
                "[EMAIL_10]", "ten@example.com"
        ));

        // when
        service.sendDelta(emitter, MailDraftPhase.BODY, "수신자 [EMAIL_10], 참조 [EMAIL_1]", restoreContext);

        // then
        assertEquals("수신자 ten@example.com, 참조 one@example.com", emitter.deltaPayload().delta());
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
        assertEquals("제목", emitter.deltaPayload().delta());
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
        assertEquals("본문", emitter.deltaPayload().delta());
        assertEquals(new MailDraftUsageResult("gpt-test", 20, 7, 27), result);
    }

    @Test
    void combined스트림은태그를파싱해서subject와body이벤트로전송한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(
                response("<SUB", "gpt-test", usage(30, 5)),
                response("JECT>\n제목\n</SUBJECT>\n<BODY>\n안녕", "gpt-test", usage(30, 10)),
                response("하세요</BO", "gpt-test", usage(30, 15)),
                response("DY>", "gpt-test", usage(30, 18))
        );
        MailDraftCommandService service = createService(chatModel);

        // when
        MailDraftUsageResult result = service.streamCombined(
                emitter,
                prompt(),
                new MailDraftRestoreContextResult(null),
                service.createCancellation()
        );

        // then
        assertEquals(List.of("subject", "body", "body"), emitter.eventNames());
        assertEquals("제목", emitter.deltaAt(0));
        assertEquals("안녕하", emitter.deltaAt(1));
        assertEquals("세요", emitter.deltaAt(2));
        assertEquals(new MailDraftUsageResult("gpt-test", 30, 18, 48), result);
    }

    @Test
    void combined스트림포맷이깨지면전용예외를던진다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("제목\n본문", "gpt-test", usage(10, 3)));
        MailDraftCommandService service = createService(chatModel);

        // when & then
        assertThrows(MailDraftCommandService.MailDraftCombinedFormatException.class, () -> service.streamCombined(
                emitter,
                prompt(),
                new MailDraftRestoreContextResult(null),
                service.createCancellation()
        ));
        assertEquals(List.of(), emitter.eventNames());
    }

    @Test
    void 요청모델을Prompt옵션에설정한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("제목", "anthropic/claude-sonnet-4.6", usage(10, 3)));
        MailDraftCommandService service = createService(chatModel);

        // when
        service.streamSubject(
                emitter,
                prompt(),
                new MailDraftRestoreContextResult(null),
                service.createCancellation(),
                "anthropic/claude-sonnet-4.6"
        );

        // then
        var captor = forClass(Prompt.class);
        verify(chatModel).stream(captor.capture());
        assertEquals("anthropic/claude-sonnet-4.6", captor.getValue().getOptions().getModel());
    }

    @Test
    void 허용되지않은모델이면실패한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("제목", "unknown/model", usage(10, 3)));
        MailDraftCommandService service = createService(chatModel);

        // when & then
        assertThrows(AiModelException.class, () -> service.streamSubject(
                emitter,
                prompt(),
                new MailDraftRestoreContextResult(null),
                service.createCancellation(),
                "unknown/model"
        ));
    }

    @Test
    void 마스킹토큰이chunk경계에서잘려도정확히복원한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(
                response("수신자 [EMA", "gpt-test", usage(10, 3)),
                response("IL_1]님", "gpt-test", usage(10, 5))
        );
        MailDraftCommandService service = createService(chatModel);
        MailDraftRestoreContextResult context = new MailDraftRestoreContextResult(Map.of("[EMAIL_1]", "alice@example.com"));

        // when
        service.streamBody(emitter, prompt(), context);

        // then
        assertEquals("수신자 alice@example.com님", emitter.joinedDeltas());
    }

    @Test
    void 일반대괄호텍스트는placeholder로판단하지않는다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        MailDraftCommandService service = createService();

        // when
        service.sendDelta(emitter, MailDraftPhase.SUBJECT, "[캡스톤디자인] 출석 인정 요청");

        // then
        assertEquals("[캡스톤디자인] 출석 인정 요청", emitter.deltaPayload().delta());
    }

    @Test
    void 모델이임의placeholder를만들면제거하고스트림을계속한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("감사합니다.\n[사용자 이름] 드림", "gpt-test", usage(10, 3)));
        MailDraftCommandService service = createService(chatModel);

        // when
        service.streamBody(emitter, prompt());

        // then
        assertEquals("감사합니다.\n", emitter.joinedDeltas());
    }

    @Test
    void 임의placeholder가chunk경계에서잘려도치환하고스트림을계속한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("지원한 [사용자 이름", "gpt-test", usage(10, 3)), response("]입니다", "gpt-test", usage(10, 5)));
        MailDraftCommandService service = createService(chatModel);

        // when
        service.streamBody(emitter, prompt());

        // then
        assertEquals("지원한 입니다", emitter.joinedDeltas());
    }

    @Test
    void 학생이름placeholder는학생으로치환한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("수강하고 있는 [학생 이름]입니다.", "gpt-test", usage(10, 3)));
        MailDraftCommandService service = createService(chatModel);

        // when
        service.streamBody(emitter, prompt());

        // then
        assertEquals("수강하고 있는 학생입니다.", emitter.joinedDeltas());
    }

    @Test
    void 복원되지않은마스킹토큰은계속실패시킨다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        ChatModel chatModel = chatModel(response("수신자 [EMAIL_1]", "gpt-test", usage(10, 3)));
        MailDraftCommandService service = createService(chatModel);

        // when
        MailDraftException exception = assertThrows(MailDraftException.class, () -> service.streamBody(emitter, prompt()));

        // then
        assertEquals(MailDraftErrorCode.UNRESOLVED_PLACEHOLDER, exception.getErrorCode());
    }

    @Test
    void 취소되면이후delta와잔여버퍼를전송하지않는다() {
        // given
        ChatModel chatModel = chatModel(response("안녕 [EMA", "gpt-test", usage(10, 3)), response("IL_1]", "gpt-test", usage(10, 5)));
        MailDraftCommandService service = createService(chatModel);
        MailDraftCommandService.StreamCancellation cancellation = service.createCancellation();
        CapturingSseEmitter emitter = new CancelingSseEmitter(service, cancellation);
        MailDraftRestoreContextResult context = new MailDraftRestoreContextResult(Map.of("[EMAIL_1]", "alice@example.com"));

        // when
        service.streamBody(emitter, prompt(), context, cancellation);

        // then
        assertEquals("안녕 ", emitter.joinedDeltas());
    }

    @Test
    void usage이벤트는subject와body사용량을합산해서전송한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        MailDraftCommandService service = createService();

        // when
        service.sendUsage(emitter, usageResult("gpt-test", 10, 3), usageResult("gpt-test", 20, 7));

        // then
        assertEquals("usage", emitter.eventName());
        assertEquals(new MailDraftUsageEvent("gpt-test", 30, 10, 40), emitter.payload());
    }

    @Test
    void done이벤트를전송한다() {
        // given
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        MailDraftCommandService service = createService();

        // when
        service.sendDone(emitter);

        // then
        assertEquals("done", emitter.eventName());
        assertEquals(MailDraftDoneEvent.success(), emitter.payload());
    }

    @Test
    void error이벤트전송실패는예외로전파하지않는다() {
        // given
        FailingSseEmitter emitter = new FailingSseEmitter();
        MailDraftCommandService service = createService();

        // when & then
        assertDoesNotThrow(() -> service.sendError(emitter, new RuntimeException("failed")));
    }

    private MailDraftCommandService createService() {
        return new MailDraftCommandService(mock(MailDraftRateLimitCachePort.class), chatModelProvider(), modelProperties());
    }

    private MailDraftCommandService createService(ChatModel chatModel) {
        return new MailDraftCommandService(mock(MailDraftRateLimitCachePort.class), chatModelProvider(chatModel), modelProperties());
    }

    private AiModelProperties modelProperties() {
        AiModelProperties properties = new AiModelProperties();
        properties.setDefaultModel("google/gemini-3.5-flash");
        properties.setAllowedModels(List.of(
                "google/gemini-3.5-flash",
                "google/gemini-3.1-pro-preview",
                "google/gemini-3.1-flash-lite",
                "google/gemini-3-pro-preview",
                "google/gemini-3-flash-preview",
                "google/gemini-2.5-pro",
                "google/gemini-2.5-flash",
                "google/gemini-2.5-flash-lite",
                "openai/gpt-5.5",
                "openai/gpt-5.5-pro",
                "openai/gpt-5.4-nano",
                "openai/gpt-5.4-mini",
                "openai/gpt-5.4",
                "openai/gpt-5.4-pro",
                "openai/gpt-5.3-chat",
                "openai/gpt-5.3-codex",
                "openai/gpt-5.2-chat",
                "openai/gpt-5.2",
                "openai/gpt-5.1",
                "openai/gpt-4.1",
                "openai/gpt-4.1-mini",
                "openai/gpt-4.1-nano",
                "qwen/qwen3.6-flash",
                "qwen/qwen3.5-plus-20260420",
                "anthropic/claude-haiku-4.5",
                "anthropic/claude-sonnet-4.6",
                "anthropic/claude-sonnet-4.5",
                "anthropic/claude-sonnet-4",
                "anthropic/claude-opus-4.7",
                "anthropic/claude-opus-4.7-fast",
                "anthropic/claude-opus-4.6",
                "anthropic/claude-opus-4.6-fast",
                "anthropic/claude-opus-4.1",
                "anthropic/claude-opus-4",
                "mistralai/mistral-medium-3-5",
                "x-ai/grok-4.3"
        ));
        return properties;
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

    private ChatModel chatModel(ChatResponse... responses) {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(responses));
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

    private MailDraftUsageResult usageResult(String model, int inputTokens, int outputTokens) {
        return new MailDraftUsageResult(model, inputTokens, outputTokens, inputTokens + outputTokens);
    }

    private static class CapturingSseEmitter extends SseEmitter {

        private final List<CapturedEvent> events = new ArrayList<>();
        private String eventName;
        private Object payload;

        @Override
        public synchronized void send(SseEventBuilder builder) {
            CapturedEvent event = capture(builder);
            this.events.add(event);
            this.eventName = event.name();
            this.payload = event.data();
        }

        private String eventName() {
            return eventName;
        }

        private MailDraftDeltaEvent deltaPayload() {
            return (MailDraftDeltaEvent) payload;
        }

        private String joinedDeltas() {
            StringBuilder builder = new StringBuilder();
            for (CapturedEvent event : events) {
                builder.append(((MailDraftDeltaEvent) event.data()).delta());
            }
            return builder.toString();
        }

        private Object payload() {
            return payload;
        }

        private List<String> eventNames() {
            return events.stream()
                    .map(CapturedEvent::name)
                    .toList();
        }

        private String deltaAt(int index) {
            return ((MailDraftDeltaEvent) events.get(index).data()).delta();
        }

        private CapturedEvent capture(SseEventBuilder builder) {
            String eventName = null;
            Object eventData = null;
            for (ResponseBodyEmitter.DataWithMediaType data : builder.build()) {
                eventName = captureEventName(eventName, data.getData());
                eventData = captureEventData(eventData, data.getData());
            }
            return new CapturedEvent(eventName, eventData);
        }

        private String captureEventName(String currentName, Object data) {
            if (data instanceof String text && text.startsWith("event:")) {
                return parseEventName(text);
            }
            return currentName;
        }

        private String parseEventName(String text) {
            return text.substring("event:".length(), eventNameEndIndex(text)).trim();
        }

        private int eventNameEndIndex(String text) {
            int endIndex = text.indexOf('\n');
            if (endIndex < 0) {
                return text.length();
            }
            return endIndex;
        }

        private Object captureEventData(Object currentData, Object data) {
            if (data instanceof String) {
                return currentData;
            }
            return data;
        }
    }

    private static final class CancelingSseEmitter extends CapturingSseEmitter {

        private final MailDraftCommandService service;
        private final MailDraftCommandService.StreamCancellation cancellation;

        private CancelingSseEmitter(MailDraftCommandService service,
                                    MailDraftCommandService.StreamCancellation cancellation) {
            this.service = service;
            this.cancellation = cancellation;
        }

        @Override
        public synchronized void send(SseEventBuilder builder) {
            super.send(builder);
            service.cancel(cancellation);
        }
    }

    private static final class FailingSseEmitter extends SseEmitter {

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            throw new IOException("disconnected");
        }
    }

    private record TestUsage(Integer getPromptTokens, Integer getCompletionTokens) implements Usage {

        public Object getNativeUsage() {
            return null;
        }
    }

    private record CapturedEvent(String name, Object data) {
    }
}
