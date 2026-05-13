package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftPhase;
import com.mailsangja.core.dto.mail.MailDraftPromptResult;
import com.mailsangja.core.dto.mail.MailDraftRagContextResult;
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
        MailDraftUsageResult subjectUsage = mailDraftCommandService.streamSubject(emitter, prompt);
        mailDraftCommandService.recordSuccess(command, MailDraftPhase.SUBJECT, subjectUsage);
        streamBody(emitter, command, prompt);
    }

    private void streamBody(SseEmitter emitter, MailDraftCommand command, MailDraftPromptResult prompt) {
        try {
            MailDraftUsageResult bodyUsage = mailDraftCommandService.streamBody(emitter, prompt);
            mailDraftCommandService.recordSuccess(command, MailDraftPhase.BODY, bodyUsage);
        } catch (Exception exception) {
            mailDraftCommandService.recordFailure(command, MailDraftPhase.BODY, exception);
            throw exception;
        }
    }
}
