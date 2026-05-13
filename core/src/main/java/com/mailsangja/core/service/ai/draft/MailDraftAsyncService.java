package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRagContextResult;
import com.mailsangja.core.dto.mail.MailDraftRestoreContextResult;
import com.mailsangja.core.dto.mail.MailDraftUsageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class MailDraftAsyncService {

    private final MailDraftQueryService mailDraftQueryService;
    private final MailDraftCommandService mailDraftCommandService;

    @Async
    public void streamGeneral(SseEmitter emitter, MailDraftCommand command) {
        registerCancelOnCompletion(emitter, command);
        MailDraftRagContextResult context = mailDraftQueryService.generalRagContext(command);
        MailDraftPromptResult prompt = mailDraftQueryService.generalPrompt(command, context);
        stream(emitter, command, prompt);
    }

    @Async
    public void streamReply(SseEmitter emitter, MailDraftCommand command) {
        registerCancelOnCompletion(emitter, command);
        MailDraftRagContextResult context = mailDraftQueryService.replyRagContext(command);
        MailDraftPromptResult prompt = mailDraftQueryService.replyPrompt(command, context);
        stream(emitter, command, prompt);
    }

    private void registerCancelOnCompletion(SseEmitter emitter, MailDraftCommand command) {
        emitter.onCompletion(new Runnable() {
            public void run() {
                mailDraftCommandService.cancel(command);
            }
        });
    }

    private void stream(SseEmitter emitter, MailDraftCommand command, MailDraftPromptResult prompt) {
        try {
            streamAfterRateLimit(emitter, command, prompt);
        } catch (Exception exception) {
            mailDraftCommandService.sendError(emitter, exception);
            mailDraftCommandService.complete(emitter);
        }
    }

    private void streamAfterRateLimit(SseEmitter emitter, MailDraftCommand command, MailDraftPromptResult prompt) {
        mailDraftCommandService.validateMonthlyRateLimit(command.userId());
        MailDraftRestoreContextResult restoreContext = MailDraftRestoreContextResult.from(command);
        MailDraftUsageResult subjectUsage = mailDraftCommandService.streamSubject(emitter, prompt, restoreContext);
        mailDraftCommandService.recordSuccess(command, MailDraftPhase.SUBJECT, subjectUsage);
        MailDraftUsageResult bodyUsage = streamBody(emitter, command, prompt, restoreContext);
        completeSuccess(emitter, subjectUsage, bodyUsage);
    }

    private MailDraftUsageResult streamBody(SseEmitter emitter, MailDraftCommand command, MailDraftPromptResult prompt,
                                            MailDraftRestoreContextResult restoreContext) {
        try {
            MailDraftUsageResult bodyUsage = mailDraftCommandService.streamBody(emitter, prompt, restoreContext);
            mailDraftCommandService.recordSuccess(command, MailDraftPhase.BODY, bodyUsage);
            return bodyUsage;
        } catch (Exception exception) {
            mailDraftCommandService.recordFailure(command, MailDraftPhase.BODY, exception);
            throw exception;
        }
    }

    private void completeSuccess(SseEmitter emitter, MailDraftUsageResult subjectUsage, MailDraftUsageResult bodyUsage) {
        mailDraftCommandService.sendUsage(emitter, subjectUsage, bodyUsage);
        mailDraftCommandService.sendDone(emitter);
        mailDraftCommandService.complete(emitter);
    }
}
