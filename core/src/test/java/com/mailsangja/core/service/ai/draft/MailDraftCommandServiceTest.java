package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
import com.mailsangja.db.port.MailDraftRateLimitCachePort;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailDraftCommandServiceTest {

    @Test
    void 월간50회까지는허용한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailDraftRateLimitCachePort cachePort = mock(MailDraftRateLimitCachePort.class);
        MailDraftCommandService service = new MailDraftCommandService(cachePort);
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
        MailDraftCommandService service = new MailDraftCommandService(cachePort);
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

    private MailDraftCommandService createService() {
        return new MailDraftCommandService(mock(MailDraftRateLimitCachePort.class));
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
}
