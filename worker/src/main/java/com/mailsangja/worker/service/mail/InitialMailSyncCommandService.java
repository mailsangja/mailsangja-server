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
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncAttachmentResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageSaveCommand;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncSaveResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InitialMailSyncCommandService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    @Transactional
    public InitialMailSyncSaveResult saveThreadBatch(MailAccount mailAccount, List<InitialMailSyncThreadSaveCommand> commands) {
        if (mailAccount == null || commands == null || commands.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }

        List<UUID> savedThreadIds = new ArrayList<>();
        List<UUID> savedMessageIds = new ArrayList<>();
        int threadMessageCount = 0;
        for (InitialMailSyncThreadSaveCommand command : commands) {
            InitialMailSyncSaveResult result = saveThread(mailAccount, command);
            savedThreadIds.addAll(result.threadIds());
            savedMessageIds.addAll(result.messageIds());
            threadMessageCount += result.threadMessageCount();
        }
        return new InitialMailSyncSaveResult(savedThreadIds, savedMessageIds, threadMessageCount);
    }

    @Transactional
    public void saveMissingMessagesFromThreadSnapshot(MailAccount mailAccount, InitialMailSyncThreadSaveCommand command) {
        if (mailAccount == null || command == null || isBlank(command.gmailThreadId()) || command.messages() == null) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }

        command.messages().stream()
                .collect(Collectors.groupingBy(InitialMailSyncMessageSaveCommand::direction))
                .forEach((direction, messageCommands) -> saveMissingMessagesByDirection(
                        mailAccount,
                        command,
                        direction,
                        messageCommands
                ));
    }

    private InitialMailSyncSaveResult saveThread(MailAccount mailAccount, InitialMailSyncThreadSaveCommand command) {
        if (command == null || isBlank(command.gmailThreadId()) || command.messages() == null) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        List<ThreadDirectionSaveResult> results = command.messages().stream()
                .collect(Collectors.groupingBy(InitialMailSyncMessageSaveCommand::direction))
                .entrySet().stream()
                .map(entry -> saveThreadDirection(mailAccount, command, entry.getKey(), entry.getValue()))
                .toList();
        return new InitialMailSyncSaveResult(
                results.stream().map(ThreadDirectionSaveResult::threadId).toList(),
                results.stream().flatMap(result -> result.messageIds().stream()).toList(),
                synchronizeThreadMessageCount(mailAccount.getId(), command.gmailThreadId())
        );
    }

    private ThreadDirectionSaveResult saveThreadDirection(
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
        List<UUID> savedMessageIds = new ArrayList<>();

        for (InitialMailSyncMessageSaveCommand messageCommand : messageCommands) {
            saveMessage(thread, threadCommand, messageCommand, aggregate)
                    .ifPresent(savedMessageIds::add);
        }

        thread.updateHistoryId(aggregate.historyId());
        thread.updateLatestMessageInfoIfNewer(
                aggregate.latestSubject(),
                aggregate.latestSnippet(),
                aggregate.latestParticipantAddress(),
                aggregate.latestParticipantName(),
                aggregate.lastMessageAt(),
                aggregate.read()
        );
        return new ThreadDirectionSaveResult(thread.getId(), savedMessageIds);
    }

    private void saveMissingMessagesByDirection(
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
        int insertedCount = 0;

        for (InitialMailSyncMessageSaveCommand messageCommand : messageCommands) {
            if (messageCommand == null
                    || isBlank(messageCommand.gmailMessageId())
                    || isBlank(messageCommand.fromAddress())) {
                throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
            }

            Optional<Message> anyMessage = messageRepositoryPort.findByThreadIdAndGmailMessageId(
                    thread.getId(),
                    messageCommand.gmailMessageId()
            );
            if (anyMessage.isPresent()) {
                // 활성 메시지는 이미 존재, 소프트 삭제된 메시지는 재삽입하지 않고 skip
                if (!anyMessage.get().isDeleted()) {
                    aggregate.merge(threadCommand, messageCommand, false);
                }
                continue;
            }

            Message message = Message.from(thread, messageCommand.toCreateValues());
            message.replaceAttachments(createAttachments(message, messageCommand.attachments()));
            messageRepositoryPort.save(message);
            aggregate.merge(threadCommand, messageCommand, true);
            insertedCount++;
        }

        thread.updateHistoryId(aggregate.historyId());
        thread.updateLatestMessageInfoIfNewer(
                aggregate.latestSubject(),
                aggregate.latestSnippet(),
                aggregate.latestParticipantAddress(),
                aggregate.latestParticipantName(),
                aggregate.lastMessageAt(),
                aggregate.read()
        );

        if (insertedCount > 0) {
            synchronizeThreadMessageCount(mailAccount.getId(), threadCommand.gmailThreadId());
        }
    }

    private int synchronizeThreadMessageCount(UUID mailAccountId, String gmailThreadId) {
        List<Message> activeMessages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccountId,
                gmailThreadId
        );
        int activeCount = activeMessages.size();
        List<Thread> threads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccountId,
                gmailThreadId
        );
        threads.forEach(thread -> thread.updateMessageCount(activeCount));
        return activeCount;
    }

    private Optional<UUID> saveMessage(
            Thread thread,
            InitialMailSyncThreadSaveCommand threadCommand,
            InitialMailSyncMessageSaveCommand messageCommand,
            ThreadAggregate aggregate
    ) {
        validateThreadMessage(threadCommand, messageCommand);

        Optional<Message> anyMessage = messageRepositoryPort.findByThreadIdAndGmailMessageId(
                thread.getId(),
                messageCommand.gmailMessageId()
        );

        if (anyMessage.isEmpty()) {
            Message message = Message.from(thread, messageCommand.toCreateValues());
            message.replaceAttachments(createAttachments(message, messageCommand.attachments()));
            Message savedMessage = messageRepositoryPort.save(message);
            aggregate.merge(threadCommand, messageCommand, true);
            return Optional.ofNullable(savedMessage.getId());
        } else if (!anyMessage.get().isDeleted()) {
            Message message = anyMessage.get();
            message.updateFrom(messageCommand.toCreateValues());
            message.replaceAttachments(createAttachments(message, messageCommand.attachments()));
            aggregate.merge(threadCommand, messageCommand, false);
            return Optional.ofNullable(message.getId());
        }
        // 소프트 삭제된 메시지는 건너뛴다 (의도적으로 삭제된 메시지는 재삽입하지 않는다)
        return Optional.empty();
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
                        .contentId(attachment.contentId())
                        .disposition(attachment.disposition())
                        .size(attachment.size())
                        .build())
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ThreadDirectionSaveResult(
            UUID threadId,
            List<UUID> messageIds
    ) {
        private ThreadDirectionSaveResult {
            messageIds = messageIds == null ? List.of() : List.copyOf(messageIds);
        }
    }

    private static final class ThreadAggregate {
        private String historyId;
        private String latestSubject;
        private String latestSnippet;
        private String latestParticipantAddress;
        private String latestParticipantName;
        private LocalDateTime lastMessageAt;
        private boolean read;
        private int messageCount;

        private static ThreadAggregate from(Thread thread) {
            ThreadAggregate aggregate = new ThreadAggregate();
            aggregate.historyId = thread.getHistoryId();
            aggregate.latestSubject = thread.getLatestSubject();
            aggregate.latestSnippet = thread.getLatestSnippet();
            aggregate.latestParticipantAddress = thread.getLatestParticipantAddress();
            aggregate.latestParticipantName = thread.getLatestParticipantName();
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
                        messageCommand.toAddresses(),
                        messageCommand.ccAddresses()
                );
                latestParticipantName = resolveLatestParticipantName(
                        messageCommand.direction(),
                        messageCommand.fromName(),
                        messageCommand.toNames(),
                        messageCommand.ccNames(),
                        latestParticipantAddress
                );
                lastMessageAt = candidateSentAt;
                read = messageCommand.read();
            }
        }

        private boolean shouldReplaceLatest(LocalDateTime candidateSentAt) {
            if (candidateSentAt == null) {
                return false;
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

        private String latestParticipantName() {
            return latestParticipantName;
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

        private String resolveLatestParticipantAddress(
                Direction direction,
                String fromAddress,
                List<String> toAddresses,
                List<String> ccAddresses
        ) {
            if (direction == Direction.OUTBOUND) {
                if (toAddresses != null && !toAddresses.isEmpty()) {
                    return toAddresses.getFirst();
                }
                return ccAddresses == null || ccAddresses.isEmpty() ? null : ccAddresses.getFirst();
            }
            return fromAddress;
        }

        private String resolveLatestParticipantName(
                Direction direction,
                String fromName,
                List<String> toNames,
                List<String> ccNames,
                String participantAddress
        ) {
            if (direction == Direction.OUTBOUND) {
                if (toNames == null || toNames.isEmpty()) {
                    if (ccNames == null || ccNames.isEmpty()) {
                        return participantAddress;
                    }
                    return normalizeName(ccNames.getFirst(), participantAddress);
                }
                return normalizeName(toNames.getFirst(), participantAddress);
            }
            return normalizeName(fromName, participantAddress);
        }

        private String normalizeName(String name, String fallbackAddress) {
            if (name == null) {
                return fallbackAddress;
            }

            String trimmedName = name.trim();
            if (trimmedName.isBlank()) {
                return fallbackAddress;
            }

            return trimmedName;
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
