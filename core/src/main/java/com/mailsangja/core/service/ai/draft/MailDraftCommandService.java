package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftDeltaEvent;
import com.mailsangja.core.dto.mail.MailDraftDoneEvent;
import com.mailsangja.core.dto.mail.MailDraftErrorEvent;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageEvent;
import com.mailsangja.core.dto.mail.MailDraftUsageResult;
import com.mailsangja.db.port.MailDraftRateLimitCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class MailDraftCommandService {

    private final MailDraftRateLimitCachePort rateLimitCachePort;
    private final ObjectProvider<ChatModel> chatModelProvider;

    public void validateMonthlyRateLimit(UUID userId) {
        validateMonthlyCount(rateLimitCachePort.incrementMonthlyCount(userId));
    }

    private void validateMonthlyCount(long monthlyCount) {
        if (monthlyCount > 50) {
            throw new MailDraftException(MailDraftErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    public MailDraftUsageResult streamSubject(SseEmitter emitter, MailDraftPromptResult prompt) {
        return streamSubject(emitter, prompt, new MailDraftRestoreContextResult(null));
    }

    public MailDraftUsageResult streamSubject(SseEmitter emitter, MailDraftPromptResult prompt, MailDraftRestoreContextResult restoreContext) {
        return streamSubject(emitter, prompt, restoreContext, createCancellation());
    }

    public MailDraftUsageResult streamSubject(SseEmitter emitter, MailDraftPromptResult prompt,
                                              MailDraftRestoreContextResult restoreContext,
                                              StreamCancellation cancellation) {
        return streamPhase(emitter, prompt, MailDraftPhase.SUBJECT, restoreContext, cancellation);
    }

    public MailDraftUsageResult streamBody(SseEmitter emitter, MailDraftPromptResult prompt) {
        return streamBody(emitter, prompt, new MailDraftRestoreContextResult(null));
    }

    public MailDraftUsageResult streamBody(SseEmitter emitter, MailDraftPromptResult prompt, MailDraftRestoreContextResult restoreContext) {
        return streamBody(emitter, prompt, restoreContext, createCancellation());
    }

    public MailDraftUsageResult streamBody(SseEmitter emitter, MailDraftPromptResult prompt,
                                           MailDraftRestoreContextResult restoreContext,
                                           StreamCancellation cancellation) {
        return streamPhase(emitter, prompt, MailDraftPhase.BODY, restoreContext, cancellation);
    }

    private MailDraftUsageResult streamPhase(SseEmitter emitter, MailDraftPromptResult prompt, MailDraftPhase phase,
                                             MailDraftRestoreContextResult restoreContext,
                                             StreamCancellation cancellation) {
        if (cancellation.isCancelled()) {
            return usageOf((ChatResponse) null);
        }
        ChatResponse lastResponse = null;
        Prompt phasePrompt = createPrompt(prompt, phase);
        MailDraftTokenBoundaryBuffer buffer = tokenBuffer(restoreContext);
        for (ChatResponse response : cancellableStream(phasePrompt, cancellation).toIterable()) {
            if (cancellation.isCancelled()) {
                break;
            }
            lastResponse = response;
            emitSafeDelta(emitter, phase, buffer.append(textOf(response)), restoreContext);
        }
        flushIfActive(emitter, phase, restoreContext, cancellation, buffer);
        return usageOf(lastResponse);
    }

    private Flux<ChatResponse> cancellableStream(Prompt prompt, StreamCancellation cancellation) {
        return chatModel().stream(prompt).takeUntilOther(cancellation.cancelSignal());
    }

    private void flushIfActive(SseEmitter emitter, MailDraftPhase phase, MailDraftRestoreContextResult restoreContext,
                               StreamCancellation cancellation, MailDraftTokenBoundaryBuffer buffer) {
        if (cancellation.isCancelled()) {
            return;
        }
        emitSafeDelta(emitter, phase, buffer.finish(), restoreContext);
    }

    private Prompt createPrompt(MailDraftPromptResult prompt, MailDraftPhase phase) {
        List<Message> messages = List.of(
                new SystemMessage(prompt.systemPrompt()),
                new UserMessage(userPrompt(prompt, phase))
        );
        return new Prompt(messages);
    }

    private String userPrompt(MailDraftPromptResult prompt, MailDraftPhase phase) {
        if (phase == MailDraftPhase.SUBJECT) {
            return prompt.userPrompt() + "\n\nReturn only the email subject.";
        }
        return prompt.userPrompt() + "\n\nReturn only the email body.";
    }

    private MailDraftTokenBoundaryBuffer tokenBuffer(MailDraftRestoreContextResult restoreContext) {
        if (restoreContext == null || restoreContext.tokens() == null) {
            return new MailDraftTokenBoundaryBuffer(Set.of());
        }
        return new MailDraftTokenBoundaryBuffer(restoreContext.tokens().keySet());
    }

    private ChatModel chatModel() {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new MailDraftException(MailDraftErrorCode.CHAT_MODEL_NOT_AVAILABLE);
        }
        return chatModel;
    }

    private String textOf(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return safeText(response.getResult().getOutput().getText());
    }

    private String safeText(String text) {
        if (text == null) {
            return "";
        }
        return text;
    }

    private void emitSafeDelta(SseEmitter emitter, MailDraftPhase phase, String delta,
                               MailDraftRestoreContextResult restoreContext) {
        if (!delta.isEmpty()) {
            sendDelta(emitter, phase, delta, restoreContext);
        }
    }

    public void sendDelta(SseEmitter emitter, MailDraftPhase phase, String delta) {
        sendDelta(emitter, phase, delta, new MailDraftRestoreContextResult(null));
    }

    public void sendDelta(SseEmitter emitter, MailDraftPhase phase, String delta, MailDraftRestoreContextResult restoreContext) {
        MailDraftDeltaEvent event = new MailDraftDeltaEvent(phase, restoreAndValidate(delta, restoreContext));
        send(emitter, event(eventName(phase), event));
    }

    private String restoreAndValidate(String delta, MailDraftRestoreContextResult restoreContext) {
        String restored = restore(delta, restoreContext);
        validateNoUnresolvedPlaceholder(restored);
        return restored;
    }

    private String restore(String delta, MailDraftRestoreContextResult restoreContext) {
        if (restoreContext == null || restoreContext.tokens() == null) {
            return delta;
        }
        return restoreTokens(delta, restoreContext);
    }

    private String restoreTokens(String delta, MailDraftRestoreContextResult restoreContext) {
        String restored = delta;
        for (String token : restoreContext.tokens().keySet()) {
            restored = restored.replace(token, restoreContext.tokens().get(token));
        }
        return restored;
    }

    private void validateNoUnresolvedPlaceholder(String text) {
        int startIndex = text.indexOf('[');
        while (startIndex >= 0) {
            int endIndex = text.indexOf(']', startIndex + 1);
            validatePlaceholderEnd(endIndex);
            throw new MailDraftException(MailDraftErrorCode.UNRESOLVED_PLACEHOLDER);
        }
    }

    private void validatePlaceholderEnd(int endIndex) {
        if (endIndex < 0) {
            throw new MailDraftException(MailDraftErrorCode.UNRESOLVED_PLACEHOLDER);
        }
    }

    private String eventName(MailDraftPhase phase) {
        if (phase == MailDraftPhase.SUBJECT) {
            return "subject";
        }
        return "body";
    }

    private MailDraftUsageResult usageOf(ChatResponse response) {
        ChatResponseMetadata metadata = metadataOf(response);
        Usage usage = usageOf(metadata);
        return new MailDraftUsageResult(modelOf(metadata), promptTokens(usage), completionTokens(usage), totalTokens(usage));
    }

    private ChatResponseMetadata metadataOf(ChatResponse response) {
        if (response == null) {
            return ChatResponseMetadata.builder().build();
        }
        return response.getMetadata();
    }

    private Usage usageOf(ChatResponseMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        return metadata.getUsage();
    }

    private String modelOf(ChatResponseMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        return metadata.getModel();
    }

    private int promptTokens(Usage usage) {
        if (usage == null || usage.getPromptTokens() == null) {
            return 0;
        }
        return usage.getPromptTokens();
    }

    private int completionTokens(Usage usage) {
        if (usage == null || usage.getCompletionTokens() == null) {
            return 0;
        }
        return usage.getCompletionTokens();
    }

    private int totalTokens(Usage usage) {
        if (usage == null || usage.getTotalTokens() == null) {
            return 0;
        }
        return usage.getTotalTokens();
    }

    public void sendUsage(SseEmitter emitter, MailDraftUsageResult subjectUsage, MailDraftUsageResult bodyUsage) {
        send(emitter, event("usage", MailDraftUsageEvent.of(subjectUsage, bodyUsage)));
    }

    public void sendDone(SseEmitter emitter) {
        send(emitter, event("done", MailDraftDoneEvent.success()));
    }

    public void complete(SseEmitter emitter) {
        emitter.complete();
    }

    public void sendError(SseEmitter emitter, Exception exception) {
        send(emitter, event("error", MailDraftErrorEvent.from(exception)));
    }

    public StreamCancellation createCancellation() {
        return new StreamCancellation();
    }

    public void cancel(StreamCancellation cancellation) {
        cancellation.cancel();
    }

    private SseEmitter.SseEventBuilder event(String name, Object data) {
        return SseEmitter.event().name(name).data(data);
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder builder) {
        try {
            emitter.send(builder);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static final class StreamCancellation {

        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final Sinks.Empty<Void> cancelSink = Sinks.empty();

        private void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                cancelSink.tryEmitEmpty();
            }
        }

        private Mono<Void> cancelSignal() {
            return cancelSink.asMono();
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    private static final class MailDraftTokenBoundaryBuffer {

        private final Set<String> tokens;
        private final StringBuilder pending = new StringBuilder();

        private MailDraftTokenBoundaryBuffer(Set<String> tokens) {
            this.tokens = tokens;
        }

        private String append(String delta) {
            pending.append(delta);
            return flushSafeText();
        }

        private String finish() {
            String text = pending.toString();
            pending.setLength(0);
            return text;
        }

        private String flushSafeText() {
            int keepLength = Math.max(partialTokenLength(), partialBracketLength());
            return flush(pending.length() - keepLength);
        }

        private String flush(int flushEnd) {
            String text = pending.substring(0, flushEnd);
            pending.delete(0, flushEnd);
            return text;
        }

        private int partialTokenLength() {
            int length = Math.min(maxTokenLength() - 1, pending.length());
            while (length > 0) {
                if (isPartialToken(pending.substring(pending.length() - length))) {
                    return length;
                }
                length--;
            }
            return 0;
        }

        private int maxTokenLength() {
            int max = 1;
            for (String token : tokens) {
                max = Math.max(max, token.length());
            }
            return max;
        }

        private boolean isPartialToken(String suffix) {
            for (String token : tokens) {
                if (token.startsWith(suffix) && !token.equals(suffix)) {
                    return true;
                }
            }
            return false;
        }

        private int partialBracketLength() {
            int startIndex = pending.lastIndexOf("[");
            if (startIndex < 0 || hasClosingBracketAfter(startIndex)) {
                return 0;
            }
            return pending.length() - startIndex;
        }

        private boolean hasClosingBracketAfter(int startIndex) {
            return pending.indexOf("]", startIndex + 1) >= 0;
        }
    }
}
