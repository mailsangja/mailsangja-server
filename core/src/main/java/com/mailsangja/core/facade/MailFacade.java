package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.mail.MailAttachmentDownloadResult;
import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.service.google.GoogleMailAttachmentQueryService;
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.MailAttachmentQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.mail.MailCommandService;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MailFacade {

    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    private final MailCommandService mailCommandService;
    private final MailAttachmentQueryService mailAttachmentQueryService;
    private final GoogleMailAttachmentQueryService googleMailAttachmentQueryService;

    public void sendMail(User user, MailSendRequest request) {
        validateRequest(request);
        request.validate();

        MailSendCommand command = MailSendCommand.from(user, request);
        var persistCommand = mailCommandService.sendMail(command);
        mailCommandService.saveSentMail(persistCommand);
    }

    public void replyMail(User user, UUID messageId, MailSendRequest request) {
        validateRequest(request);
        request.validate();

        Message replyTargetMessage = mailCommandService.findReplyTargetMessage(messageId);
        validateReplyTargetAccess(user, replyTargetMessage);

        MailSendCommand command = MailSendCommand.from(user, request);
        var persistCommand = mailCommandService.replyMail(command, replyTargetMessage);
        mailCommandService.saveSentMail(persistCommand);
    }

    public MailAttachmentDownloadResult getAttachment(User user, UUID attachmentId) {
        validateAttachmentId(attachmentId);

        Attachment attachment = mailAttachmentQueryService.findById(attachmentId);
        validateAttachmentAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), attachment);
        validateAttachmentProvider(attachment);

        MailAccount ensuredMailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
                attachment.getMessage().getThread().getMailAccount()
        );
        byte[] attachmentBytes = googleMailAttachmentQueryService.download(
                ensuredMailAccount,
                attachment.getMessage(),
                attachment
        );

        return new MailAttachmentDownloadResult(
                attachment.getFilename(),
                attachment.getMimeType(),
                attachmentBytes
        );
    }

    private void validateRequest(MailSendRequest request) {
        if (request == null) {
            throw new MailSendException(MailSendErrorCode.INVALID_MAIL_REQUEST);
        }
    }

    private void validateAttachmentId(UUID attachmentId) {
        if (attachmentId == null) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_NOT_FOUND);
        }
    }

    private void validateAttachmentAccess(List<MailAccount> userAccounts, Attachment attachment) {
        Set<UUID> userAccountIds = userAccounts.stream()
                .map(MailAccount::getId)
                .collect(Collectors.toSet());

        if (!userAccountIds.contains(attachment.getMessage().getThread().getMailAccount().getId())) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_ACCESS_DENIED);
        }
    }

    private void validateAttachmentProvider(Attachment attachment) {
        if (attachment.getMessage().getThread().getMailAccount().getProvider() != MailProvider.GMAIL) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_PROVIDER_NOT_SUPPORTED);
        }
    }

    private void validateReplyTargetAccess(User user, Message replyTargetMessage) {
        MailAccount replyTargetMailAccount = replyTargetMessage.getThread().getMailAccount();
        if (user == null
                || replyTargetMailAccount == null
                || replyTargetMailAccount.getUser() == null
                || !user.getId().equals(replyTargetMailAccount.getUser().getId())) {
            throw new MailSendException(MailSendErrorCode.REPLY_TARGET_MESSAGE_ACCESS_DENIED);
        }
    }
}
