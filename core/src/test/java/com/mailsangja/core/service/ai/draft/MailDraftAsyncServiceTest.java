package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRagContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailDraftAsyncServiceTest {

    @Test
    void 클라이언트가Sse연결을끊으면LLM스트리밍을취소한다() {
        // given
        Fixture fixture = createFixture();
        TestSseEmitter emitter = new TestSseEmitter();
        MailDraftCommand command = createCommand();

        // when
        fixture.asyncService().streamGeneral(emitter, command);
        emitter.disconnect();

        // then
        verify(fixture.commandService()).cancel(command);
    }

    @Test
    void subject성공후body실패시subject사용량과body실패를기록하고error를보낸다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();
        MailDraftUsageResult subjectUsage = new MailDraftUsageResult("gpt-4o-mini", 10, 5, 15);
        when(fixture.commandService().streamSubject(eq(emitter), any(), any())).thenReturn(subjectUsage);
        doThrow(new RuntimeException("body failed")).when(fixture.commandService()).streamBody(eq(emitter), any(), any());

        // when
        fixture.asyncService().streamGeneral(emitter, command);

        // then
        verify(fixture.commandService()).recordSuccess(command, MailDraftPhase.SUBJECT, subjectUsage);
        verify(fixture.commandService()).recordFailure(eq(command), eq(MailDraftPhase.BODY), any());
        verify(fixture.commandService()).sendError(eq(emitter), any());
        verify(fixture.commandService()).complete(emitter);
    }

    @Test
    void subject와body성공시usage와done을보내고emitter를완료한다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();
        MailDraftUsageResult subjectUsage = new MailDraftUsageResult("gpt-4o-mini", 10, 5, 15);
        MailDraftUsageResult bodyUsage = new MailDraftUsageResult("gpt-4o-mini", 20, 10, 30);
        when(fixture.commandService().streamSubject(eq(emitter), any(), any())).thenReturn(subjectUsage);
        when(fixture.commandService().streamBody(eq(emitter), any(), any())).thenReturn(bodyUsage);

        // when
        fixture.asyncService().streamGeneral(emitter, command);

        // then
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(fixture.commandService());
        inOrder.verify(fixture.commandService()).sendUsage(emitter, subjectUsage, bodyUsage);
        inOrder.verify(fixture.commandService()).sendDone(emitter);
        inOrder.verify(fixture.commandService()).complete(emitter);
    }

    @Test
    void rateLimit은템플릿조립후LLM호출직전에소모한다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();

        // when
        fixture.asyncService().streamGeneral(emitter, command);

        // then
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(fixture.queryService(), fixture.commandService());
        inOrder.verify(fixture.queryService()).generalPrompt(eq(command), any());
        inOrder.verify(fixture.commandService()).validateMonthlyRateLimit(command.userId());
        inOrder.verify(fixture.commandService()).streamSubject(eq(emitter), any(), any());
    }

    @Test
    void rateLimit에걸리면LLM을호출하지않고error를보낸다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();
        doThrow(mock(MailDraftException.class)).when(fixture.commandService()).validateMonthlyRateLimit(command.userId());

        // when
        fixture.asyncService().streamGeneral(emitter, command);

        // then
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamSubject(any(), any(), any());
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamBody(any(), any(), any());
        verify(fixture.commandService()).sendError(eq(emitter), any());
        verify(fixture.commandService()).complete(emitter);
    }

    @Test
    void general은일반템플릿으로조립한최종프롬프트로LLM을호출한다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftPromptResult prompt = new MailDraftPromptResult("general system", "general user");
        when(fixture.queryService().generalPrompt(any(), any())).thenReturn(prompt);

        // when
        fixture.asyncService().streamGeneral(emitter, createCommand());

        // then
        var captor = forClass(MailDraftPromptResult.class);
        verify(fixture.commandService()).streamSubject(eq(emitter), captor.capture(), any());
        assertSame(prompt, captor.getValue());
    }

    @Test
    void reply는답장템플릿으로조립한최종프롬프트로LLM을호출한다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftPromptResult prompt = new MailDraftPromptResult("reply system", "reply user");
        MailDraftCommand command = createReplyCommand();
        when(fixture.queryService().replyRagContext(command)).thenReturn(MailDraftRagContextResult.empty());
        when(fixture.queryService().replyPrompt(eq(command), any())).thenReturn(prompt);

        // when
        fixture.asyncService().streamReply(emitter, command);

        // then
        var captor = forClass(MailDraftPromptResult.class);
        verify(fixture.commandService()).streamSubject(eq(emitter), captor.capture(), any());
        assertSame(prompt, captor.getValue());
    }

    private Fixture createFixture() {
        MailDraftQueryService queryService = mock(MailDraftQueryService.class);
        MailDraftCommandService commandService = mock(MailDraftCommandService.class);
        MailDraftAsyncService asyncService = new MailDraftAsyncService(queryService, commandService);
        stubDraftPrompt(queryService);
        return new Fixture(asyncService, queryService, commandService);
    }

    private void stubDraftPrompt(MailDraftQueryService queryService) {
        when(queryService.generalRagContext(any())).thenReturn(MailDraftRagContextResult.empty());
        when(queryService.generalPrompt(any(), any())).thenReturn(new MailDraftPromptResult("system", "user"));
    }

    private MailDraftCommand createCommand() {
        return new MailDraftCommand(UUID.randomUUID(), UUID.randomUUID(), "masked query", null, List.of("to@example.com"), List.of());
    }

    private MailDraftCommand createReplyCommand() {
        return new MailDraftCommand(UUID.randomUUID(), UUID.randomUUID(), "masked query", UUID.randomUUID(), List.of("to@example.com"), List.of());
    }

    private record Fixture(
            MailDraftAsyncService asyncService,
            MailDraftQueryService queryService,
            MailDraftCommandService commandService
    ) {
    }

    private static final class TestSseEmitter extends SseEmitter {

        private Runnable completionCallback = () -> {
        };

        @Override
        public synchronized void onCompletion(Runnable callback) {
            this.completionCallback = callback;
        }

        private void disconnect() {
            completionCallback.run();
        }
    }
}
