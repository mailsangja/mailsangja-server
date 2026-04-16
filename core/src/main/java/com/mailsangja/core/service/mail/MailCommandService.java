package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.dto.mail.GoogleMailAttachmentResult;
import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.core.dto.mail.MailSendPersistCommand;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.core.service.google.GoogleMailMessageQueryService;
import com.mailsangja.core.service.google.GoogleMailSendCommandService;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MailCommandService {

    private final MailQueryService mailQueryService;
    private final GoogleMailSendCommandService googleMailSendCommandService;
    private final GoogleMailMessageQueryService googleMailMessageQueryService;
    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    public MailSendPersistCommand sendMail(MailSendCommand command) {
        MailAccount senderMailAccount = mailQueryService.findActiveSenderMailAccount(
                command.userId(),
                command.from().address()
        );

        var sendResult = googleMailSendCommandService.send(senderMailAccount, command);
        GoogleMailMessageResult messageResult = googleMailMessageQueryService.getMessage(
                senderMailAccount.getAccessToken(),
                sendResult.gmailMessageId()
        );
        validateMessageResult(sendResult.gmailMessageId(), sendResult.gmailThreadId(), messageResult);

        return new MailSendPersistCommand(senderMailAccount, messageResult, command);
    }

    @Transactional
    public void saveSentMail(MailSendPersistCommand command) {
        validatePersistCommand(command);

        Thread thread = findOrCreateThread(command.mailAccount(), command.messageResult().gmailThreadId());
        gmailThreadLockRepositoryPort.acquireThreadLock(command.mailAccount(), command.messageResult().gmailThreadId());
        boolean inserted = saveMessage(thread, command);

        thread.updateHistoryId(command.messageResult().historyId());
        thread.updateLatestMessageInfoIfNewer(
                command.messageResult().subject(),
                command.messageResult().snippet(),
                command.latestParticipantAddress(),
                command.messageResult().sentAt(),
                true
        );

        if (inserted) {
            thread.updateMessageCount(thread.getMessageCount() + 1);
        }
    }

    private void validateMessageResult(String gmailMessageId, String gmailThreadId, GoogleMailMessageResult messageResult) {
        if (messageResult == null
                || isBlank(messageResult.gmailMessageId())
                || isBlank(messageResult.gmailThreadId())
                || !gmailMessageId.equals(messageResult.gmailMessageId())
                || !gmailThreadId.equals(messageResult.gmailThreadId())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }
    }

    private void validatePersistCommand(MailSendPersistCommand command) {
        if (command == null
                || command.mailAccount() == null
                || command.messageResult() == null
                || isBlank(command.messageResult().gmailThreadId())
                || isBlank(command.messageResult().gmailMessageId())
                || isBlank(command.messageResult().fromAddress())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }
    }

    private Thread findOrCreateThread(MailAccount mailAccount, String gmailThreadId) {
        return threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                        mailAccount.getId(),
                        gmailThreadId,
                        Direction.OUTBOUND
                )
                .orElseGet(() -> threadRepositoryPort.save(Thread.builder()
                        .mailAccount(mailAccount)
                        .gmailThreadId(gmailThreadId)
                        .direction(Direction.OUTBOUND)
                        .read(true)
                        .messageCount(0)
                        .build()));
    }

    private boolean saveMessage(Thread thread, MailSendPersistCommand command) {
        Message message = messageRepositoryPort.findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                        thread.getId(),
                        command.messageResult().gmailMessageId()
                )
                .orElse(null);

        if (message == null) {
            Message newMessage = Message.from(thread, command.toCreateValues());
            newMessage.replaceAttachments(createAttachments(newMessage, command.messageResult().attachments()));
            messageRepositoryPort.save(newMessage);
            return true;
        }

        message.updateFrom(command.toCreateValues());
        message.replaceAttachments(createAttachments(message, command.messageResult().attachments()));
        messageRepositoryPort.save(message);
        return false;
    }

    private List<Attachment> createAttachments(
            Message message,
            List<GoogleMailAttachmentResult> attachments
    ) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .map(attachment -> Attachment.builder()
                        .message(message)
                        .gmailAttachmentId(attachment.gmailAttachmentId())
                        .filename(attachment.filename())
                        .mimeType(attachment.mimeType())
                        .size(attachment.size())
                        .build())
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
