package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
import com.mailsangja.core.dto.mail.GoogleMailReplyContextResult;
import com.mailsangja.core.dto.mail.GoogleMailSendResult;
import com.mailsangja.core.dto.mail.MailAttachmentDownloadResult;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.core.dto.mail.MailSendPersistCommand;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.service.google.GoogleMailAttachmentQueryService;
import com.mailsangja.core.service.google.GoogleMailMessageQueryService;
import com.mailsangja.core.service.google.GoogleMailSendCommandService;
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.MailAttachmentQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.mail.MailCommandService;
import com.mailsangja.core.service.mail.MailQueryService;
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
    private final MailQueryService mailQueryService;
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    private final GoogleMailSendCommandService googleMailSendCommandService;
    private final GoogleMailMessageQueryService googleMailMessageQueryService;
    private final MailCommandService mailCommandService;
    private final MailAttachmentQueryService mailAttachmentQueryService;
    private final GoogleMailAttachmentQueryService googleMailAttachmentQueryService;

    public void sendMail(User user, MailSendRequest request) {
        MailSendRequest.validate(request);

        MailSendCommand command = MailSendCommand.from(user, request);
        MailAccount senderMailAccount = mailQueryService.findActiveSenderMailAccount(
                command.userId(),
                command.from().address()
        );
        MailAccount ensuredMailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(senderMailAccount);
        GoogleMailSendResult sendResult = googleMailSendCommandService.send(ensuredMailAccount, command);
        GoogleMailSendResult.validate(sendResult);
        GoogleMailMessageResult messageResult = googleMailMessageQueryService.getMessage(
                ensuredMailAccount.getAccessToken(),
                sendResult.gmailMessageId()
        );
        MailSendPersistCommand persistCommand = MailSendPersistCommand.of(
                ensuredMailAccount,
                command,
                sendResult,
                messageResult
        );
        mailCommandService.saveSentMail(persistCommand);
    }

    public void replyMail(User user, UUID messageId, MailSendRequest request) {
        MailSendRequest.validate(request);

        MailSendCommand command = MailSendCommand.from(user, request);
        Message replyTargetMessage = mailQueryService.findReplyTargetMessage(messageId);
        command.validateReplyTargetAccess(replyTargetMessage);

        MailAccount senderMailAccount = mailQueryService.findActiveSenderMailAccount(
                command.userId(),
                command.from().address()
        );
        command.validateReplySender(senderMailAccount, replyTargetMessage);

        MailAccount ensuredMailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(senderMailAccount);
        GoogleMailSendResult sendResult = googleMailSendCommandService.reply(
                ensuredMailAccount,
                command,
                GoogleMailReplyContextResult.from(replyTargetMessage)
        );
        GoogleMailSendResult.validate(sendResult);
        GoogleMailMessageResult messageResult = googleMailMessageQueryService.getMessage(
                ensuredMailAccount.getAccessToken(),
                sendResult.gmailMessageId()
        );
        MailSendPersistCommand persistCommand = MailSendPersistCommand.of(
                ensuredMailAccount,
                command,
                sendResult,
                messageResult
        );
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
}
