package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRagContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageResult;
import com.mailsangja.db.entity.user.Plan;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
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
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);
        emitter.disconnect();

        // then
        verify(fixture.commandService()).cancel(fixture.cancellation());
    }

    @Test
    void sse타임아웃이면LLM스트리밍을취소한다() {
        // given
        Fixture fixture = createFixture();
        TestSseEmitter emitter = new TestSseEmitter();
        MailDraftCommand command = createCommand();

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);
        emitter.timeout();

        // then
        verify(fixture.commandService()).cancel(fixture.cancellation());
    }

    @Test
    void sse전송오류이면LLM스트리밍을취소한다() {
        // given
        Fixture fixture = createFixture();
        TestSseEmitter emitter = new TestSseEmitter();
        MailDraftCommand command = createCommand();

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);
        emitter.fail();

        // then
        verify(fixture.commandService()).cancel(fixture.cancellation());
    }

    @Test
    void subject성공후body실패시error를보내고완료한다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();
        MailDraftUsageResult subjectUsage = new MailDraftUsageResult("gpt-4o-mini", 10, 5, 15);
        when(fixture.commandService().streamSubject(eq(emitter), any(), any(), any(), any())).thenReturn(subjectUsage);
        doThrow(new RuntimeException("body failed")).when(fixture.commandService()).streamBody(eq(emitter), any(), any(), any(), any());

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);

        // then
        verify(fixture.commandService()).sendError(eq(emitter), any());
        verify(fixture.commandService()).complete(emitter);
    }

    @Test
    void error이벤트전송실패시에도emitter를완료한다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();
        when(fixture.commandService().streamSubject(eq(emitter), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("subject failed"));
        doThrow(new IllegalStateException("send failed")).when(fixture.commandService()).sendError(eq(emitter), any());

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);

        // then
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
        when(fixture.commandService().streamSubject(eq(emitter), any(), any(), any(), any())).thenReturn(subjectUsage);
        when(fixture.commandService().streamBody(eq(emitter), any(), any(), any(), any())).thenReturn(bodyUsage);

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);

        // then
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(fixture.commandService());
        inOrder.verify(fixture.commandService()).sendUsage(emitter, subjectUsage, bodyUsage);
        inOrder.verify(fixture.commandService()).sendDone(emitter);
        inOrder.verify(fixture.commandService()).complete(emitter);
    }

    @Test
    void combined성공시단일usage와done을보내고분리스트림을호출하지않는다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();
        MailDraftUsageResult usage = new MailDraftUsageResult("gpt-4o-mini", 30, 15, 45);
        org.mockito.Mockito.doReturn(usage).when(fixture.commandService()).streamCombined(eq(emitter), any(), any(), any(), any());

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);

        // then
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(fixture.commandService());
        inOrder.verify(fixture.commandService()).sendUsage(emitter, usage);
        inOrder.verify(fixture.commandService()).sendDone(emitter);
        inOrder.verify(fixture.commandService()).complete(emitter);
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamSubject(any(), any(), any(), any(), any());
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamBody(any(), any(), any(), any(), any());
    }

    @Test
    void subject중취소되면body와완료이벤트를보내지않는다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();
        MailDraftUsageResult subjectUsage = new MailDraftUsageResult("gpt-4o-mini", 10, 5, 15);
        when(fixture.commandService().streamSubject(eq(emitter), any(), any(), any(), any())).thenAnswer(new Answer<MailDraftUsageResult>() {
            public MailDraftUsageResult answer(InvocationOnMock invocation) {
                fixture.commandService().cancel(fixture.cancellation());
                return subjectUsage;
            }
        });

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);

        // then
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamBody(any(), any(), any(), any(), any());
        verify(fixture.commandService(), org.mockito.Mockito.never()).sendDone(emitter);
    }

    @Test
    void rateLimit은템플릿조립후LLM호출직전에소모한다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);

        // then
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(fixture.queryService(), fixture.commandService());
        inOrder.verify(fixture.queryService()).generalPrompt(eq(command), any());
        inOrder.verify(fixture.commandService()).resolveModel(command.model());
        inOrder.verify(fixture.commandService()).validateWeeklyRateLimit(command.userId(), Plan.FREE);
        inOrder.verify(fixture.commandService()).streamSubject(eq(emitter), any(), any(), any(), any());
    }

    @Test
    void rateLimit에걸리면LLM을호출하지않고error를보낸다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftCommand command = createCommand();
        doThrow(mock(MailDraftException.class)).when(fixture.commandService()).validateWeeklyRateLimit(command.userId(), Plan.FREE);

        // when
        fixture.asyncService().streamGeneral(emitter, command, Plan.FREE);

        // then
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamSubject(any(), any(), any(), any(), any());
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamBody(any(), any(), any(), any(), any());
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
        fixture.asyncService().streamGeneral(emitter, createCommand(), Plan.FREE);

        // then
        var captor = forClass(MailDraftPromptResult.class);
        verify(fixture.commandService()).streamSubject(eq(emitter), captor.capture(), any(), any(), any());
        assertSame(prompt, captor.getValue());
    }

    @Test
    void reply는답장템플릿으로본문만LLM호출한다() {
        // given
        Fixture fixture = createFixture();
        SseEmitter emitter = new SseEmitter();
        MailDraftPromptResult prompt = new MailDraftPromptResult("reply system", "reply user");
        MailDraftCommand command = createReplyCommand();
        MailDraftUsageResult bodyUsage = new MailDraftUsageResult("gpt-4o-mini", 20, 10, 30);
        when(fixture.queryService().replyRagContext(command)).thenReturn(MailDraftRagContextResult.empty());
        when(fixture.queryService().replyPrompt(eq(command), any())).thenReturn(prompt);
        when(fixture.commandService().streamBody(eq(emitter), any(), any(), any(), any())).thenReturn(bodyUsage);

        // when
        fixture.asyncService().streamReply(emitter, command, Plan.FREE);

        // then
        var captor = forClass(MailDraftPromptResult.class);
        verify(fixture.commandService()).streamBody(eq(emitter), captor.capture(), any(), any(), any());
        assertSame(prompt, captor.getValue());
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamCombined(any(), any(), any(), any(), any());
        verify(fixture.commandService(), org.mockito.Mockito.never()).streamSubject(any(), any(), any(), any(), any());
        verify(fixture.commandService()).sendUsage(emitter, bodyUsage);
        verify(fixture.commandService()).sendDone(emitter);
        verify(fixture.commandService()).complete(emitter);
    }

    private Fixture createFixture() {
        MailDraftQueryService queryService = mock(MailDraftQueryService.class);
        MailDraftCommandService commandService = mock(MailDraftCommandService.class);
        MailDraftAsyncService asyncService = new MailDraftAsyncService(queryService, commandService);
        MailDraftCommandService.StreamCancellation cancellation = new MailDraftCommandService.StreamCancellation();
        when(commandService.createCancellation()).thenReturn(cancellation);
        when(commandService.streamCombined(any(), any(), any(), any(), any()))
                .thenThrow(new MailDraftCommandService.MailDraftCombinedFormatException("invalid"));
        when(commandService.resolveModel(any())).thenReturn("openai/gpt-4o-mini");
        doCallRealMethod().when(commandService).cancel(cancellation);
        stubDraftPrompt(queryService);
        return new Fixture(asyncService, queryService, commandService, cancellation);
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
            MailDraftCommandService commandService,
            MailDraftCommandService.StreamCancellation cancellation
    ) {
    }

    private static final class TestSseEmitter extends SseEmitter {

        private Runnable completionCallback = new Runnable() {
            public void run() {
            }
        };
        private Runnable timeoutCallback = new Runnable() {
            public void run() {
            }
        };
        private Consumer<Throwable> errorCallback = new Consumer<Throwable>() {
            public void accept(Throwable throwable) {
            }
        };

        @Override
        public synchronized void onCompletion(Runnable callback) {
            this.completionCallback = callback;
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            this.timeoutCallback = callback;
        }

        @Override
        public synchronized void onError(Consumer<Throwable> callback) {
            this.errorCallback = callback;
        }

        private void disconnect() {
            completionCallback.run();
        }

        private void timeout() {
            timeoutCallback.run();
        }

        private void fail() {
            errorCallback.accept(new RuntimeException("sse failed"));
        }
    }
}
