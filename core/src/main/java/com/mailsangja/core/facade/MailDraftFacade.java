package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftStreamRequest;
import com.mailsangja.core.service.ai.draft.MailDraftAsyncService;
import com.mailsangja.core.service.ai.draft.MailDraftQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.mail.MailQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class MailDraftFacade {

    private final MailAccountQueryService mailAccountQueryService;
    private final MailQueryService mailQueryService;
    private final MailDraftQueryService mailDraftQueryService;
    private final MailDraftAsyncService mailDraftAsyncService;

    public SseEmitter streamDraft(User user, MailDraftStreamRequest request) {
        MailAccount account = mailAccountQueryService.findActiveById(request.mailAccountId());
        validateAccountAccess(user, account);
        mailDraftQueryService.validatePromptInjection(request.query());
        MailDraftCommand command = mailDraftQueryService.createCommand(user.getId(), request);
        return startStream(user, command);
    }

    private void validateAccountAccess(User user, MailAccount account) {
        if (!account.getUser().getId().equals(user.getId())) {
            throw new MailDraftException(MailDraftErrorCode.ACCESS_DENIED);
        }
    }

    private SseEmitter startStream(User user, MailDraftCommand command) {
        SseEmitter emitter = new SseEmitter();
        streamByPurpose(user, emitter, command);
        return emitter;
    }

    private void streamByPurpose(User user, SseEmitter emitter, MailDraftCommand command) {
        if (command.purpose() == com.mailsangja.core.dto.mail.MailDraftPurpose.GENERAL) {
            mailDraftAsyncService.streamGeneral(emitter, command);
            return;
        }
        streamReply(user, emitter, command);
    }

    private void streamReply(User user, SseEmitter emitter, MailDraftCommand command) {
        Message replyTarget = mailQueryService.findReplyTargetMessage(command.replyMessageId());
        validateReplyTarget(user, replyTarget);
        mailDraftAsyncService.streamReply(emitter, command);
    }

    private void validateReplyTarget(User user, Message replyTarget) {
        if (replyTarget == null || replyTarget.isDeleted()) {
            throw new MailDraftException(MailDraftErrorCode.ACCESS_DENIED);
        }
        validateReplyTargetOwner(user, replyTarget);
    }

    private void validateReplyTargetOwner(User user, Message replyTarget) {
        MailAccount account = replyTarget.getThread().getMailAccount();
        validateAccountAccess(user, account);
    }
}
