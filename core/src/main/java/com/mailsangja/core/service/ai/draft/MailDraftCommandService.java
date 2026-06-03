package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.config.properties.AiModelProperties;
import com.mailsangja.core.dto.mail.MailDraftDeltaEvent;
import com.mailsangja.core.dto.mail.MailDraftDoneEvent;
import com.mailsangja.core.dto.mail.MailDraftErrorEvent;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageEvent;
import com.mailsangja.core.dto.mail.MailDraftUsageResult;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.port.MailDraftRateLimitCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailDraftCommandService {

    private static final Pattern GENERATED_PLACEHOLDER_SIGNATURE_PATTERN = Pattern.compile(
            "(?m)^\\s*\\[[^\\]\\n]*(?:사용자|이름|담당자|회사|소속|직함|전화|연락처|이메일)[^\\]\\n]*]\\s*(?:드림|올림|배상)?\\s*$\\R?"
    );
    private static final String COMBINED_DRAFT_FORMAT_INSTRUCTION = """

            Return exactly in this streaming format:
            <SUBJECT>
            one email subject only
            </SUBJECT>
            <BODY>
            email body only
            </BODY>

            Rules:
            - Start with <SUBJECT>.
            - Write nothing outside <SUBJECT>, </SUBJECT>, <BODY>, and </BODY>.
            - After </SUBJECT>, immediately write <BODY>.
            - Do not use markdown.
            - Do not include the tags inside the subject or body content.
            - Do not include any new placeholder, template variable, fill-in blank, or signature name if unknown.
            """;

    private final MailDraftRateLimitCachePort rateLimitCachePort;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final AiModelProperties modelProperties;

    public void validateWeeklyRateLimit(UUID userId) {
        validateWeeklyRateLimit(userId, Plan.FREE);
    }

    public void validateWeeklyRateLimit(UUID userId, Plan plan) {
        boolean allowed = rateLimitCachePort.tryConsumeWeeklyLimit(userId);
        if (plan != Plan.PRO && !allowed) {
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
        return streamSubject(emitter, prompt, restoreContext, cancellation, null);
    }

    public MailDraftUsageResult streamSubject(SseEmitter emitter, MailDraftPromptResult prompt,
                                              MailDraftRestoreContextResult restoreContext,
                                              StreamCancellation cancellation,
                                              String model) {
        return streamPhase(emitter, prompt, MailDraftPhase.SUBJECT, restoreContext, cancellation, resolveModel(model));
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
        return streamBody(emitter, prompt, restoreContext, cancellation, null);
    }

    public MailDraftUsageResult streamBody(SseEmitter emitter, MailDraftPromptResult prompt,
                                           MailDraftRestoreContextResult restoreContext,
                                           StreamCancellation cancellation,
                                           String model) {
        return streamPhase(emitter, prompt, MailDraftPhase.BODY, restoreContext, cancellation, resolveModel(model));
    }

    public MailDraftUsageResult streamCombined(SseEmitter emitter, MailDraftPromptResult prompt,
                                               MailDraftRestoreContextResult restoreContext,
                                               StreamCancellation cancellation) {
        return streamCombined(emitter, prompt, restoreContext, cancellation, null);
    }

    public MailDraftUsageResult streamCombined(SseEmitter emitter, MailDraftPromptResult prompt,
                                               MailDraftRestoreContextResult restoreContext,
                                               StreamCancellation cancellation,
                                               String model) {
        if (cancellation.isCancelled()) {
            return usageOf((ChatResponse) null);
        }
        ChatResponse lastResponse = null;
        Prompt combinedPrompt = createCombinedPrompt(prompt, resolveModel(model));
        MailDraftCombinedStreamParser parser = new MailDraftCombinedStreamParser();
        MailDraftTokenBoundaryBuffer subjectBuffer = tokenBuffer(restoreContext);
        MailDraftTokenBoundaryBuffer bodyBuffer = tokenBuffer(restoreContext);
        for (ChatResponse response : cancellableStream(combinedPrompt, cancellation).toIterable()) {
            if (cancellation.isCancelled()) {
                break;
            }
            lastResponse = response;
            emitParsedDeltas(emitter, parser.append(textOf(response)), subjectBuffer, bodyBuffer, restoreContext);
        }
        if (!cancellation.isCancelled()) {
            emitParsedDeltas(emitter, parser.finish(), subjectBuffer, bodyBuffer, restoreContext);
            flushIfActive(emitter, MailDraftPhase.SUBJECT, restoreContext, cancellation, subjectBuffer);
            flushIfActive(emitter, MailDraftPhase.BODY, restoreContext, cancellation, bodyBuffer);
        }
        return usageOf(lastResponse);
    }

    private MailDraftUsageResult streamPhase(SseEmitter emitter, MailDraftPromptResult prompt, MailDraftPhase phase,
                                             MailDraftRestoreContextResult restoreContext,
                                             StreamCancellation cancellation,
                                             String model) {
        if (cancellation.isCancelled()) {
            return usageOf((ChatResponse) null);
        }
        ChatResponse lastResponse = null;
        Prompt phasePrompt = createPrompt(prompt, phase, model);
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

    public String resolveModel(String requestedModel) {
        return modelProperties.resolveDraft(requestedModel);
    }

    private Prompt createPrompt(MailDraftPromptResult prompt, MailDraftPhase phase, String model) {
        List<Message> messages = List.of(
                new SystemMessage(prompt.systemPrompt()),
                new UserMessage(userPrompt(prompt, phase))
        );
        return new Prompt(messages, OpenAiChatOptions.builder()
                .model(model)
                .build());
    }

    private Prompt createCombinedPrompt(MailDraftPromptResult prompt, String model) {
        List<Message> messages = List.of(
                new SystemMessage(prompt.systemPrompt()),
                new UserMessage(prompt.userPrompt() + COMBINED_DRAFT_FORMAT_INSTRUCTION)
        );
        return new Prompt(messages, OpenAiChatOptions.builder()
                .model(model)
                .build());
    }

    private String userPrompt(MailDraftPromptResult prompt, MailDraftPhase phase) {
        if (phase == MailDraftPhase.SUBJECT) {
            return prompt.userPrompt() + "\n\nReturn only the email subject. Do not include any new placeholder or template variable.";
        }
        return prompt.userPrompt() + "\n\nReturn only the email body. Do not include any new placeholder, template variable, fill-in blank, or signature name if unknown.";
    }

    private MailDraftTokenBoundaryBuffer tokenBuffer(MailDraftRestoreContextResult restoreContext) {
        if (restoreContext == null || restoreContext.tokens() == null) {
            return new MailDraftTokenBoundaryBuffer(Set.of());
        }
        return new MailDraftTokenBoundaryBuffer(restoreContext.tokens().keySet());
    }

    private void emitParsedDeltas(SseEmitter emitter, List<ParsedDraftDelta> deltas,
                                  MailDraftTokenBoundaryBuffer subjectBuffer, MailDraftTokenBoundaryBuffer bodyBuffer,
                                  MailDraftRestoreContextResult restoreContext) {
        for (ParsedDraftDelta delta : deltas) {
            emitParsedDelta(emitter, delta, subjectBuffer, bodyBuffer, restoreContext);
        }
    }

    private void emitParsedDelta(SseEmitter emitter, ParsedDraftDelta delta,
                                 MailDraftTokenBoundaryBuffer subjectBuffer, MailDraftTokenBoundaryBuffer bodyBuffer,
                                 MailDraftRestoreContextResult restoreContext) {
        if (delta.phase() == MailDraftPhase.SUBJECT) {
            emitSafeDelta(emitter, delta.phase(), subjectBuffer.append(delta.text()), restoreContext);
            return;
        }
        emitSafeDelta(emitter, delta.phase(), bodyBuffer.append(delta.text()), restoreContext);
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
        String restored = restoreAndSanitize(delta, restoreContext);
        if (restored.isEmpty()) {
            return;
        }
        log.debug("Mail draft SSE delta send. phase={} length={}", phase, restored.length());
        MailDraftDeltaEvent event = new MailDraftDeltaEvent(phase, restored);
        send(emitter, event(eventName(phase), event));
    }

    private String restoreAndSanitize(String delta, MailDraftRestoreContextResult restoreContext) {
        String restored = restore(delta, restoreContext);
        return sanitizeGeneratedPlaceholders(delta, restored);
    }

    private String restore(String delta, MailDraftRestoreContextResult restoreContext) {
        if (restoreContext == null || restoreContext.tokens() == null) {
            return delta;
        }
        return restoreTokens(delta, restoreContext);
    }

    private String restoreTokens(String delta, MailDraftRestoreContextResult restoreContext) {
        String restored = delta;
        for (String token : orderedRestoreTokens(restoreContext)) {
            restored = restored.replace(token, restoreContext.tokens().get(token));
        }
        return restored;
    }

    private List<String> orderedRestoreTokens(MailDraftRestoreContextResult restoreContext) {
        return restoreContext.tokens().keySet().stream()
                .sorted(java.util.Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    private String sanitizeGeneratedPlaceholders(String rawDelta, String restoredDelta) {
        String signatureSanitized = sanitizeGeneratedPlaceholderSignature(rawDelta, restoredDelta);
        StringBuilder builder = new StringBuilder();
        int searchIndex = 0;
        while (searchIndex < signatureSanitized.length()) {
            int startIndex = signatureSanitized.indexOf('[', searchIndex);
            if (startIndex < 0) {
                builder.append(signatureSanitized.substring(searchIndex));
                break;
            }
            builder.append(signatureSanitized, searchIndex, startIndex);
            searchIndex = appendSanitizedBracketText(rawDelta, signatureSanitized, startIndex, builder);
        }
        return builder.toString();
    }

    private String sanitizeGeneratedPlaceholderSignature(String rawDelta, String restoredDelta) {
        Matcher matcher = GENERATED_PLACEHOLDER_SIGNATURE_PATTERN.matcher(restoredDelta);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            logSanitizedGeneratedPlaceholder(rawDelta, restoredDelta, matcher.start(), matcher.end() - 1);
            matcher.appendReplacement(buffer, "");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private int appendSanitizedBracketText(String rawDelta, String restoredDelta, int startIndex, StringBuilder builder) {
        int endIndex = restoredDelta.indexOf(']', startIndex + 1);
        if (endIndex < 0) {
            logUnresolvedMaskingToken(rawDelta, restoredDelta, startIndex, endIndex);
            throw new MailDraftException(MailDraftErrorCode.UNRESOLVED_PLACEHOLDER);
        }

        String content = restoredDelta.substring(startIndex + 1, endIndex).strip();
        if (isMaskingToken(content)) {
            logUnresolvedMaskingToken(rawDelta, restoredDelta, startIndex, endIndex);
            throw new MailDraftException(MailDraftErrorCode.UNRESOLVED_PLACEHOLDER);
        }
        if (isGeneratedPlaceholder(content)) {
            logSanitizedGeneratedPlaceholder(rawDelta, restoredDelta, startIndex, endIndex);
            builder.append(generatedPlaceholderReplacement(content));
            return endIndex + 1;
        }

        builder.append(restoredDelta, startIndex, endIndex + 1);
        return endIndex + 1;
    }

    private boolean isMaskingToken(String content) {
        return content.matches("[A-Z]+(?:_[A-Z]+)*_\\d+");
    }

    private boolean isGeneratedPlaceholder(String content) {
        return content.contains("사용자") || content.contains("이름") || content.contains("담당자")
                || content.contains("회사") || content.contains("소속") || content.contains("직함")
                || content.contains("전화") || content.contains("연락처") || content.contains("이메일");
    }

    private String generatedPlaceholderReplacement(String content) {
        if (content.contains("학생")) {
            return "학생";
        }
        if (content.contains("담당자")) {
            return "담당자";
        }
        return "";
    }

    private void logUnresolvedMaskingToken(String rawDelta, String restoredDelta, int startIndex, int endIndex) {
        log.warn("Mail draft unresolved masking token. placeholder={} rawDelta={} restoredDelta={}",
                placeholderOf(restoredDelta, startIndex, endIndex), logPreview(rawDelta), logPreview(restoredDelta));
    }

    private void logSanitizedGeneratedPlaceholder(String rawDelta, String restoredDelta, int startIndex, int endIndex) {
        log.warn("Mail draft generated placeholder sanitized. placeholder={} rawDelta={} restoredDelta={}",
                placeholderOf(restoredDelta, startIndex, endIndex), logPreview(rawDelta), logPreview(restoredDelta));
    }

    private String placeholderOf(String text, int startIndex, int endIndex) {
        if (endIndex < 0) {
            return logPreview(text.substring(startIndex));
        }
        return logPreview(text.substring(startIndex, endIndex + 1));
    }

    private String logPreview(String text) {
        return limitLogText(redactLogText(normalizeLogText(text)));
    }

    private String normalizeLogText(String text) {
        return safeText(text).replace('\n', ' ').replace('\r', ' ').strip();
    }

    private String redactLogText(String text) {
        String redacted = text.replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+", "[EMAIL]");
        return redacted.replaceAll("\\d{2,3}[- .]?\\d{3,4}[- .]?\\d{4}", "[PHONE]");
    }

    private String limitLogText(String text) {
        if (text.length() <= 300) {
            return text;
        }
        return text.substring(0, 300) + "...";
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

    public void sendUsage(SseEmitter emitter, MailDraftUsageResult usage) {
        send(emitter, event("usage", new MailDraftUsageEvent(
                usage.model(),
                usage.inputTokens(),
                usage.outputTokens(),
                usage.totalTokens()
        )));
    }

    public void sendDone(SseEmitter emitter) {
        send(emitter, event("done", MailDraftDoneEvent.success()));
    }

    public void complete(SseEmitter emitter) {
        emitter.complete();
    }

    public void sendError(SseEmitter emitter, Exception exception) {
        trySend(emitter, event("error", MailDraftErrorEvent.from(exception)));
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
        IOException exception = trySend(emitter, builder);
        if (exception != null) {
            throw new IllegalStateException("Mail draft SSE send failed.", exception);
        }
    }

    private IOException trySend(SseEmitter emitter, SseEmitter.SseEventBuilder builder) {
        try {
            emitter.send(builder);
            return null;
        } catch (IOException exception) {
            log.warn("Mail draft SSE send failed.", exception);
            return exception;
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

    private static final class MailDraftCombinedStreamParser {

        private static final String SUBJECT_OPEN = "<SUBJECT>";
        private static final String SUBJECT_CLOSE = "</SUBJECT>";
        private static final String BODY_OPEN = "<BODY>";
        private static final String BODY_CLOSE = "</BODY>";
        private static final int MAX_PREFIX_LENGTH = 200;

        private final StringBuilder buffer = new StringBuilder();
        private final StringBuilder subject = new StringBuilder();
        private CombinedParserState state = CombinedParserState.BEFORE_SUBJECT;
        private boolean bodyStarted;

        private List<ParsedDraftDelta> append(String delta) {
            buffer.append(delta);
            return parse(false);
        }

        private List<ParsedDraftDelta> finish() {
            return parse(true);
        }

        private List<ParsedDraftDelta> parse(boolean finishing) {
            List<ParsedDraftDelta> deltas = new ArrayList<>();
            boolean progressed = true;
            while (progressed) {
                progressed = switch (state) {
                    case BEFORE_SUBJECT -> parseBeforeSubject(finishing);
                    case SUBJECT -> parseSubject(finishing);
                    case BEFORE_BODY -> parseBeforeBody(finishing, deltas);
                    case BODY -> parseBody(finishing, deltas);
                    case DONE -> false;
                };
            }
            return deltas;
        }

        private boolean parseBeforeSubject(boolean finishing) {
            int startIndex = buffer.indexOf(SUBJECT_OPEN);
            if (startIndex >= 0) {
                validateIgnorablePrefix(buffer.substring(0, startIndex));
                buffer.delete(0, startIndex + SUBJECT_OPEN.length());
                state = CombinedParserState.SUBJECT;
                return true;
            }
            if (finishing) {
                throw invalidFormat("missing subject tag");
            }
            validatePrefixWait(SUBJECT_OPEN);
            return false;
        }

        private boolean parseSubject(boolean finishing) {
            int endIndex = buffer.indexOf(SUBJECT_CLOSE);
            if (endIndex >= 0) {
                subject.append(buffer, 0, endIndex);
                buffer.delete(0, endIndex + SUBJECT_CLOSE.length());
                state = CombinedParserState.BEFORE_BODY;
                return true;
            }
            if (finishing) {
                throw invalidFormat("missing subject close tag");
            }
            keepPossibleSuffix(SUBJECT_CLOSE, subject);
            return false;
        }

        private boolean parseBeforeBody(boolean finishing, List<ParsedDraftDelta> deltas) {
            int startIndex = buffer.indexOf(BODY_OPEN);
            if (startIndex >= 0) {
                validateIgnorablePrefix(buffer.substring(0, startIndex));
                buffer.delete(0, startIndex + BODY_OPEN.length());
                deltas.add(new ParsedDraftDelta(MailDraftPhase.SUBJECT, subject.toString().strip()));
                state = CombinedParserState.BODY;
                return true;
            }
            if (finishing) {
                throw invalidFormat("missing body tag");
            }
            validatePrefixWait(BODY_OPEN);
            return false;
        }

        private boolean parseBody(boolean finishing, List<ParsedDraftDelta> deltas) {
            int endIndex = buffer.indexOf(BODY_CLOSE);
            if (endIndex >= 0) {
                addBodyDelta(deltas, buffer.substring(0, endIndex));
                buffer.delete(0, endIndex + BODY_CLOSE.length());
                buffer.setLength(0);
                state = CombinedParserState.DONE;
                return false;
            }
            if (finishing) {
                addBodyDelta(deltas, buffer.toString());
                buffer.setLength(0);
                state = CombinedParserState.DONE;
                return false;
            }
            int flushEnd = Math.max(0, buffer.length() - BODY_CLOSE.length() + 1);
            if (flushEnd == 0) {
                return false;
            }
            addBodyDelta(deltas, buffer.substring(0, flushEnd));
            buffer.delete(0, flushEnd);
            return false;
        }

        private void addBodyDelta(List<ParsedDraftDelta> deltas, String text) {
            String bodyText = normalizeInitialBodyText(text);
            if (!bodyText.isEmpty()) {
                deltas.add(new ParsedDraftDelta(MailDraftPhase.BODY, bodyText));
            }
        }

        private String normalizeInitialBodyText(String text) {
            if (bodyStarted) {
                return text;
            }
            bodyStarted = true;
            return text.replaceFirst("^\\R+", "");
        }

        private void keepPossibleSuffix(String tag, StringBuilder target) {
            int keepLength = possibleTagSuffixLength(tag);
            int flushEnd = buffer.length() - keepLength;
            if (flushEnd <= 0) {
                return;
            }
            target.append(buffer, 0, flushEnd);
            buffer.delete(0, flushEnd);
        }

        private int possibleTagSuffixLength(String tag) {
            int max = Math.min(tag.length() - 1, buffer.length());
            for (int length = max; length > 0; length--) {
                if (tag.startsWith(buffer.substring(buffer.length() - length))) {
                    return length;
                }
            }
            return 0;
        }

        private void validatePrefixWait(String tag) {
            int keepLength = possibleTagSuffixLength(tag);
            int checkEnd = buffer.length() - keepLength;
            if (checkEnd <= 0) {
                return;
            }
            String prefix = buffer.substring(0, checkEnd);
            validateIgnorablePrefix(prefix);
            if (buffer.length() > MAX_PREFIX_LENGTH) {
                throw invalidFormat("tag prefix too long");
            }
        }

        private void validateIgnorablePrefix(String prefix) {
            if (!prefix.isBlank()) {
                throw invalidFormat("unexpected content outside tags");
            }
        }

        private MailDraftCombinedFormatException invalidFormat(String reason) {
            return new MailDraftCombinedFormatException(reason);
        }
    }

    private enum CombinedParserState {
        BEFORE_SUBJECT,
        SUBJECT,
        BEFORE_BODY,
        BODY,
        DONE
    }

    private record ParsedDraftDelta(MailDraftPhase phase, String text) {
    }

    public static final class MailDraftCombinedFormatException extends RuntimeException {

        public MailDraftCombinedFormatException(String message) {
            super(message);
        }
    }
}
