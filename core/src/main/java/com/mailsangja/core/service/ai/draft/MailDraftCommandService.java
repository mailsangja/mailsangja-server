package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageResult;
import com.mailsangja.db.port.MailDraftRateLimitCachePort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class MailDraftCommandService {

    private final MailDraftRateLimitCachePort rateLimitCachePort;

    public MailDraftCommandService() {
        this.rateLimitCachePort = null;
    }

    public MailDraftCommandService(MailDraftRateLimitCachePort rateLimitCachePort) {
        this.rateLimitCachePort = rateLimitCachePort;
    }

    public void validateMonthlyRateLimit(UUID userId) {
        if (rateLimitCachePort == null) {
            return;
        }
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
        return null;
    }

    public MailDraftUsageResult streamBody(SseEmitter emitter, MailDraftPromptResult prompt) {
        return null;
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
