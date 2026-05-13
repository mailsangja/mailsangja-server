package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailDraftCommandService {

    private final MailDraftRateLimitCachePort rateLimitCachePort;
    private final ObjectProvider<ChatModel> chatModelProvider;

    public void validateMonthlyRateLimit(UUID userId) {
        validateMonthlyCount(rateLimitCachePort.incrementMonthlyCount(userId));
    }

    public void sendDelta(SseEmitter emitter, MailDraftPhase phase, String delta) {
        sendDelta(emitter, phase, delta, new MailDraftRestoreContextResult(null));
    }

    public void sendDelta(SseEmitter emitter, MailDraftPhase phase, String delta, MailDraftRestoreContextResult restoreContext) {
        String restoredDelta = restore(delta, restoreContext);
        MailDraftDeltaEvent event = new MailDraftDeltaEvent(phase, restoredDelta);
        send(emitter, new CapturedSseEventBuilder(eventName(phase), event));
    }

    public MailDraftUsageResult streamSubject(SseEmitter emitter, MailDraftPromptResult prompt) {
        return streamPhase(emitter, prompt, MailDraftPhase.SUBJECT);
    }

    public MailDraftUsageResult streamBody(SseEmitter emitter, MailDraftPromptResult prompt) {
        return streamPhase(emitter, prompt, MailDraftPhase.BODY);
    }

    public void recordSuccess(MailDraftCommand command, MailDraftPhase phase, MailDraftUsageResult usage) {
    }

    public void recordFailure(MailDraftCommand command, MailDraftPhase phase, Exception exception) {
    }

    public void sendError(SseEmitter emitter, Exception exception) {
    }

    public void complete(SseEmitter emitter) {
        emitter.complete();
    }

    public void cancel(MailDraftCommand command) {
    }

    public static CapturedEvent capture(SseEmitter.SseEventBuilder builder) {
        return ((CapturedSseEventBuilder) builder).capturedEvent();
    }

    private void validateMonthlyCount(long monthlyCount) {
        if (monthlyCount > 50) {
            throw new MailDraftException(MailDraftErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    private MailDraftUsageResult streamPhase(SseEmitter emitter, MailDraftPromptResult prompt, MailDraftPhase phase) {
        ChatResponse lastResponse = null;
        Prompt phasePrompt = createPrompt(prompt, phase);
        for (ChatResponse response : chatModel().stream(phasePrompt).toIterable()) {
            lastResponse = response;
            sendDelta(emitter, phase, responseText(response));
        }
        return usageOf(lastResponse);
    }

    private Prompt createPrompt(MailDraftPromptResult prompt, MailDraftPhase phase) {
        List<Message> messages = List.of(
                new SystemMessage(prompt.systemPrompt()),
                new UserMessage(phaseUserPrompt(prompt, phase))
        );
        return new Prompt(messages);
    }

    private ChatModel chatModel() {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new MailDraftException(MailDraftErrorCode.CHAT_MODEL_NOT_AVAILABLE);
        }
        return chatModel;
    }

    private String phaseUserPrompt(MailDraftPromptResult prompt, MailDraftPhase phase) {
        if (phase == MailDraftPhase.SUBJECT) {
            return prompt.userPrompt() + "\n\nReturn only the email subject.";
        }
        return prompt.userPrompt() + "\n\nReturn only the email body.";
    }

    private String responseText(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return "";
        }
        return response.getResult().getOutput().getText();
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

    private String eventName(MailDraftPhase phase) {
        if (phase == MailDraftPhase.SUBJECT) {
            return "subject";
        }
        return "body";
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder builder) {
        try {
            emitter.send(builder);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record CapturedEvent(String name, Object data) {
    }

    private record CapturedSseEventBuilder(String name, Object data) implements SseEmitter.SseEventBuilder {

        private CapturedEvent capturedEvent() {
            return new CapturedEvent(name, data);
        }

        public SseEmitter.SseEventBuilder id(String id) {
            return this;
        }

        public SseEmitter.SseEventBuilder name(String name) {
            return new CapturedSseEventBuilder(name, data);
        }

        public SseEmitter.SseEventBuilder reconnectTime(long reconnectTime) {
            return this;
        }

        public SseEmitter.SseEventBuilder comment(String comment) {
            return this;
        }

        public SseEmitter.SseEventBuilder data(Object data) {
            return new CapturedSseEventBuilder(name, data);
        }

        public SseEmitter.SseEventBuilder data(Object data, MediaType mediaType) {
            return data(data);
        }

        public Set<org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType> build() {
            return Set.of();
        }
    }
}
