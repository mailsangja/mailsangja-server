package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.mail.InitialMailSyncAttachmentResult;
import com.mailsangja.worker.dto.mail.InitialMailSyncMessageSaveCommand;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadSaveCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitialMailSyncCommandService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    @Transactional
    public void saveThreadBatch(MailAccount mailAccount, List<InitialMailSyncThreadSaveCommand> commands) {
        if (mailAccount == null || commands == null || commands.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }

        for (InitialMailSyncThreadSaveCommand command : commands) {
            saveThread(mailAccount, command);
        }
    }

    private void saveThread(MailAccount mailAccount, InitialMailSyncThreadSaveCommand command) {
        if (command == null || isBlank(command.gmailThreadId()) || command.messages() == null) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        for (InitialMailSyncMessageSaveCommand messageCommand : command.messages()) {
            saveMessage(mailAccount, command, messageCommand);
        }
    }

    private void saveMessage(
            MailAccount mailAccount,
            InitialMailSyncThreadSaveCommand threadCommand,
            InitialMailSyncMessageSaveCommand messageCommand
    ) {
        validateThreadMessage(threadCommand, messageCommand);

        Direction direction = messageCommand.direction();
        boolean read = messageCommand.read();
        String fromAddress = messageCommand.fromAddress();
        String subject = messageCommand.subject();
        List<String> toAddresses = messageCommand.toAddresses();
        List<String> ccAddresses = messageCommand.ccAddresses();
        LocalDateTime sentAt = messageCommand.sentAt();

        Thread thread = findOrCreateThread(
                mailAccount,
                threadCommand.gmailThreadId(),
                direction
        );

        Optional<Message> existingMessage = messageRepositoryPort.findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                thread.getId(),
                messageCommand.gmailMessageId()
        );

        if (existingMessage.isPresent()) {
            Message message = existingMessage.get();
            message.updateBasicContent(
                    subject,
                    fromAddress,
                    toAddresses,
                    ccAddresses,
                    messageCommand.snippet(),
                    read,
                    sentAt
            );
            message.updateBodyContent(messageCommand.bodyText(), messageCommand.bodyHtml());
            message.replaceAttachments(createAttachments(message, messageCommand.attachments()));
        } else {
            Message message = Message.builder()
                    .thread(thread)
                    .gmailMessageId(messageCommand.gmailMessageId())
                    .direction(direction)
                    .subject(subject)
                    .fromAddress(fromAddress)
                    .toAddresses(toAddresses)
                    .ccAddresses(ccAddresses)
                    .snippet(messageCommand.snippet())
                    .read(read)
                    .sentAt(sentAt)
                    .bodyText(messageCommand.bodyText())
                    .bodyHtml(messageCommand.bodyHtml())
                    .attachments(new ArrayList<>())
                    .labels(Collections.emptyList())
                    .build();
            message.replaceAttachments(createAttachments(message, messageCommand.attachments()));
            messageRepositoryPort.save(message);
        }

        thread.updateHistoryId(firstNonBlank(messageCommand.historyId(), threadCommand.historyId()));
        thread.updateLatestMessageInfoIfNewer(
                subject,
                messageCommand.snippet(),
                resolveLatestParticipantAddress(direction, fromAddress, toAddresses),
                sentAt,
                read
        );
        thread.updateMessageCount((int) messageRepositoryPort.countByThreadIdAndDeletedAtIsNull(thread.getId()));
    }

    private Thread findOrCreateThread(MailAccount mailAccount, String gmailThreadId, Direction direction) {
        return threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                        mailAccount.getId(),
                        gmailThreadId,
                        direction
                )
                .orElseGet(() -> threadRepositoryPort.save(Thread.builder()
                        .mailAccount(mailAccount)
                        .gmailThreadId(gmailThreadId)
                        .direction(direction)
                        .read(true)
                        .messageCount(0)
                        .build()));
    }

    private void validateThreadMessage(
            InitialMailSyncThreadSaveCommand threadCommand,
            InitialMailSyncMessageSaveCommand messageCommand
    ) {
        if (messageCommand == null
                || isBlank(threadCommand.gmailThreadId())
                || isBlank(messageCommand.gmailMessageId())
                || isBlank(messageCommand.fromAddress())) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
    }

    private List<Attachment> createAttachments(
            Message message,
            List<InitialMailSyncAttachmentResult> attachments
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

    private LocalDateTime resolveSentAt(GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse) {
        if (isBlank(messageResponse.internalDate())) {
            return null;
        }

        try {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(messageResponse.internalDate())),
                    KST_ZONE_ID
            );
        } catch (NumberFormatException e) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
    }

    private String resolveLatestParticipantAddress(Direction direction, String fromAddress, List<String> toAddresses) {
        if (direction == Direction.OUTBOUND) {
            return toAddresses.isEmpty() ? null : toAddresses.getFirst();
        }
        return fromAddress;
    }

    private String firstNonBlank(String primary, String secondary) {
        return !isBlank(primary) ? primary : secondary;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
