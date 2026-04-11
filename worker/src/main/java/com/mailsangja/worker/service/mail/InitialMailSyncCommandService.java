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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        command.messages().stream()
                .collect(Collectors.groupingBy(InitialMailSyncMessageSaveCommand::direction))
                .forEach((direction, messageCommands) -> saveThreadDirection(mailAccount, command, direction, messageCommands));
    }

    private void saveThreadDirection(
            MailAccount mailAccount,
            InitialMailSyncThreadSaveCommand threadCommand,
            Direction direction,
            List<InitialMailSyncMessageSaveCommand> messageCommands
    ) {
        if (direction == null || messageCommands == null || messageCommands.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        Thread thread = findOrCreateThread(mailAccount, threadCommand.gmailThreadId(), direction);
        ThreadAggregate aggregate = ThreadAggregate.from(thread);

        for (InitialMailSyncMessageSaveCommand messageCommand : messageCommands) {
            saveMessage(thread, threadCommand, messageCommand, aggregate);
        }

        thread.updateHistoryId(aggregate.historyId());
        thread.updateLatestMessageInfoIfNewer(
                aggregate.latestSubject(),
                aggregate.latestSnippet(),
                aggregate.latestParticipantAddress(),
                aggregate.lastMessageAt(),
                aggregate.read()
        );
        thread.updateMessageCount(aggregate.messageCount());
    }

    private void saveMessage(
            Thread thread,
            InitialMailSyncThreadSaveCommand threadCommand,
            InitialMailSyncMessageSaveCommand messageCommand,
            ThreadAggregate aggregate
    ) {
        validateThreadMessage(threadCommand, messageCommand);

        Optional<Message> existingMessage = messageRepositoryPort.findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                thread.getId(),
                messageCommand.gmailMessageId()
        );
        boolean inserted = existingMessage.isEmpty();

        if (!inserted) {
            Message message = existingMessage.get();
            message.updateBasicContent(
                    messageCommand.subject(),
                    messageCommand.fromAddress(),
                    messageCommand.toAddresses(),
                    messageCommand.ccAddresses(),
                    messageCommand.snippet(),
                    messageCommand.read(),
                    messageCommand.sentAt()
            );
            message.updateBodyContent(messageCommand.bodyText(), messageCommand.bodyHtml());
            message.replaceAttachments(createAttachments(message, messageCommand.attachments()));
        } else {
            Message message = Message.builder()
                    .thread(thread)
                    .gmailMessageId(messageCommand.gmailMessageId())
                    .direction(messageCommand.direction())
                    .subject(messageCommand.subject())
                    .fromAddress(messageCommand.fromAddress())
                    .toAddresses(messageCommand.toAddresses())
                    .ccAddresses(messageCommand.ccAddresses())
                    .snippet(messageCommand.snippet())
                    .read(messageCommand.read())
                    .sentAt(messageCommand.sentAt())
                    .bodyText(messageCommand.bodyText())
                    .bodyHtml(messageCommand.bodyHtml())
                    .attachments(new ArrayList<>())
                    .labels(Collections.emptyList())
                    .build();
            message.replaceAttachments(createAttachments(message, messageCommand.attachments()));
            messageRepositoryPort.save(message);
        }

        aggregate.merge(threadCommand, messageCommand, inserted);
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

    private static final class ThreadAggregate {
        private String historyId;
        private String latestSubject;
        private String latestSnippet;
        private String latestParticipantAddress;
        private LocalDateTime lastMessageAt;
        private boolean read;
        private int messageCount;

        private static ThreadAggregate from(Thread thread) {
            ThreadAggregate aggregate = new ThreadAggregate();
            aggregate.historyId = thread.getHistoryId();
            aggregate.latestSubject = thread.getLatestSubject();
            aggregate.latestSnippet = thread.getLatestSnippet();
            aggregate.latestParticipantAddress = thread.getLatestParticipantAddress();
            aggregate.lastMessageAt = thread.getLastMessageAt();
            aggregate.read = thread.isRead();
            aggregate.messageCount = thread.getMessageCount();
            return aggregate;
        }

        private void merge(
                InitialMailSyncThreadSaveCommand threadCommand,
                InitialMailSyncMessageSaveCommand messageCommand,
                boolean inserted
        ) {
            historyId = firstNonBlank(messageCommand.historyId(), threadCommand.historyId(), historyId);
            if (inserted) {
                messageCount++;
            }

            LocalDateTime candidateSentAt = messageCommand.sentAt();
            if (shouldReplaceLatest(candidateSentAt)) {
                latestSubject = messageCommand.subject();
                latestSnippet = messageCommand.snippet();
                latestParticipantAddress = resolveLatestParticipantAddress(
                        messageCommand.direction(),
                        messageCommand.fromAddress(),
                        messageCommand.toAddresses()
                );
                lastMessageAt = candidateSentAt;
                read = messageCommand.read();
            }
        }

        private boolean shouldReplaceLatest(LocalDateTime candidateSentAt) {
            if (candidateSentAt == null) {
                return lastMessageAt == null && latestSubject == null && latestSnippet == null;
            }

            return lastMessageAt == null || !lastMessageAt.isAfter(candidateSentAt);
        }

        private String historyId() {
            return historyId;
        }

        private String latestSubject() {
            return latestSubject;
        }

        private String latestSnippet() {
            return latestSnippet;
        }

        private String latestParticipantAddress() {
            return latestParticipantAddress;
        }

        private LocalDateTime lastMessageAt() {
            return lastMessageAt;
        }

        private boolean read() {
            return read;
        }

        private int messageCount() {
            return messageCount;
        }

        private String firstNonBlank(String... values) {
            for (String value : values) {
                if (!isBlank(value)) {
                    return value;
                }
            }
            return null;
        }

        private String resolveLatestParticipantAddress(Direction direction, String fromAddress, List<String> toAddresses) {
            if (direction == Direction.OUTBOUND) {
                return toAddresses == null || toAddresses.isEmpty() ? null : toAddresses.getFirst();
            }
            return fromAddress;
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
