package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRagContextResult;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailDraftAsyncService {

    private final MailDraftQueryService mailDraftQueryService;
    private final MailDraftCommandService mailDraftCommandService;

    @Async
    public void streamGeneral(SseEmitter emitter, MailDraftCommand command) {
        MailDraftCommandService.StreamCancellation cancellation = mailDraftCommandService.createCancellation();
        registerCancel(emitter, cancellation);
        try {
            MailDraftRagContextResult context = mailDraftQueryService.generalRagContext(command);
            MailDraftPromptResult prompt = mailDraftQueryService.generalPrompt(command, context);
            stream(emitter, command, prompt, cancellation);
        } catch (Exception exception) {
            log.error("Mail draft stream failed. userId={} mailAccountId={} purpose={}",
                    command.userId(), command.mailAccountId(), command.purpose(), exception);
            mailDraftCommandService.sendError(emitter, exception);
            mailDraftCommandService.complete(emitter);
        }
    }

    @Async
    public void streamReply(SseEmitter emitter, MailDraftCommand command) {
        MailDraftCommandService.StreamCancellation cancellation = mailDraftCommandService.createCancellation();
        registerCancel(emitter, cancellation);
        try {
            MailDraftRagContextResult context = mailDraftQueryService.replyRagContext(command);
            MailDraftPromptResult prompt = mailDraftQueryService.replyPrompt(command, context);
            stream(emitter, command, prompt, cancellation);
        } catch (Exception exception) {
            log.error("Mail draft stream failed. userId={} mailAccountId={} purpose={}",
                    command.userId(), command.mailAccountId(), command.purpose(), exception);
            mailDraftCommandService.sendError(emitter, exception);
            mailDraftCommandService.complete(emitter);
        }
    }

    private void registerCancel(SseEmitter emitter, MailDraftCommandService.StreamCancellation cancellation) {
        registerCompletionCancel(emitter, cancellation);
        registerTimeoutCancel(emitter, cancellation);
        registerErrorCancel(emitter, cancellation);
    }

    private void registerCompletionCancel(SseEmitter emitter, MailDraftCommandService.StreamCancellation cancellation) {
        emitter.onCompletion(new Runnable() {
            public void run() {
                mailDraftCommandService.cancel(cancellation);
            }
        });
    }

    private void registerTimeoutCancel(SseEmitter emitter, MailDraftCommandService.StreamCancellation cancellation) {
        emitter.onTimeout(new Runnable() {
            public void run() {
                mailDraftCommandService.cancel(cancellation);
            }
        });
    }

    private void registerErrorCancel(SseEmitter emitter, MailDraftCommandService.StreamCancellation cancellation) {
        emitter.onError(new Consumer<Throwable>() {
            public void accept(Throwable throwable) {
                mailDraftCommandService.cancel(cancellation);
            }
        });
    }

    private void stream(SseEmitter emitter, MailDraftCommand command, MailDraftPromptResult prompt,
                        MailDraftCommandService.StreamCancellation cancellation) {
        try {
            streamAfterRateLimit(emitter, command, prompt, cancellation);
        } catch (Exception exception) {
            log.error("Mail draft stream failed. userId={} mailAccountId={} purpose={}",
                    command.userId(), command.mailAccountId(), command.purpose(), exception);
            mailDraftCommandService.sendError(emitter, exception);
            mailDraftCommandService.complete(emitter);
        }
    }

    private void streamAfterRateLimit(SseEmitter emitter, MailDraftCommand command, MailDraftPromptResult prompt,
                                      MailDraftCommandService.StreamCancellation cancellation) {
        mailDraftCommandService.validateMonthlyRateLimit(command.userId());
        MailDraftRestoreContextResult restoreContext = MailDraftRestoreContextResult.from(command);
        MailDraftUsageResult subjectUsage = mailDraftCommandService.streamSubject(emitter, prompt, restoreContext, cancellation);
        if (cancellation.isCancelled()) {
            return;
        }
        MailDraftUsageResult bodyUsage = mailDraftCommandService.streamBody(emitter, prompt, restoreContext, cancellation);
        if (cancellation.isCancelled()) {
            return;
        }
        completeSuccess(emitter, subjectUsage, bodyUsage);
    }

    private void completeSuccess(SseEmitter emitter, MailDraftUsageResult subjectUsage, MailDraftUsageResult bodyUsage) {
        mailDraftCommandService.sendUsage(emitter, subjectUsage, bodyUsage);
        mailDraftCommandService.sendDone(emitter);
        mailDraftCommandService.complete(emitter);
    }
}
