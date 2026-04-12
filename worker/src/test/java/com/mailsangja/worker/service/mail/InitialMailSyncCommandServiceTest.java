package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.mail.InitialMailSyncMessageSaveCommand;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadSaveCommand;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InitialMailSyncCommandServiceTest {

    @Test
    void saveThreadBatch_doesNotIncreaseMessageCountForUpdatedMessages() {
        InMemoryThreadRepository threadRepository = new InMemoryThreadRepository();
        InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        InitialMailSyncCommandService service = new InitialMailSyncCommandService(threadRepository, messageRepository);

        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();

        service.saveThreadBatch(mailAccount, List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-1",
                List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-1",
                        Direction.INBOUND,
                        "subject",
                        "alice@example.com",
                        List.of("bob@example.com"),
                        List.of(),
                        "snippet",
                        false,
                        LocalDateTime.of(2026, 4, 11, 10, 0),
                        "body",
                        null,
                        List.of()
                ))
        )));

        service.saveThreadBatch(mailAccount, List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-2",
                List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-2",
                        Direction.INBOUND,
                        "updated subject",
                        "alice@example.com",
                        List.of("bob@example.com"),
                        List.of(),
                        "updated snippet",
                        true,
                        LocalDateTime.of(2026, 4, 11, 11, 0),
                        "updated body",
                        null,
                        List.of()
                ))
        )));

        Thread savedThread = threadRepository.savedThreads.getFirst();
        assertEquals(1, savedThread.getMessageCount());
        assertEquals("updated subject", savedThread.getLatestSubject());
        assertEquals("updated snippet", savedThread.getLatestSnippet());
    }

    @Test
    void saveThreadBatch_doesNotReplaceLatestMessageWhenSentAtIsNull() {
        InMemoryThreadRepository threadRepository = new InMemoryThreadRepository();
        InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        InitialMailSyncCommandService service = new InitialMailSyncCommandService(threadRepository, messageRepository);

        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();

        service.saveThreadBatch(mailAccount, List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-1",
                List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-1",
                        Direction.INBOUND,
                        "subject-1",
                        "alice@example.com",
                        List.of(),
                        List.of(),
                        "snippet-1",
                        false,
                        LocalDateTime.of(2026, 4, 11, 10, 0),
                        null,
                        null,
                        List.of()
                ))
        )));

        service.saveThreadBatch(mailAccount, List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-2",
                List.of(new InitialMailSyncMessageSaveCommand(
                        "message-2",
                        "history-2",
                        Direction.INBOUND,
                        "subject-2",
                        "carol@example.com",
                        List.of(),
                        List.of(),
                        "snippet-2",
                        true,
                        null,
                        null,
                        null,
                        List.of()
                ))
        )));

        Thread savedThread = threadRepository.savedThreads.getFirst();
        assertEquals("subject-1", savedThread.getLatestSubject());
        assertEquals("snippet-1", savedThread.getLatestSnippet());
        assertEquals(LocalDateTime.of(2026, 4, 11, 10, 0), savedThread.getLastMessageAt());
        assertEquals(2, savedThread.getMessageCount());
    }

    private static final class InMemoryThreadRepository implements ThreadRepositoryPort {
        private final List<Thread> savedThreads = new ArrayList<>();

        @Override
        public Thread save(Thread thread) {
            if (!savedThreads.contains(thread)) {
                savedThreads.add(thread);
            }
            return thread;
        }

        @Override
        public Optional<Thread> findByIdAndDeletedAtIsNull(UUID id) {
            return savedThreads.stream().filter(thread -> id.equals(thread.getId())).findFirst();
        }

        @Override
        public Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId, Direction direction) {
            return savedThreads.stream()
                    .filter(thread -> thread.getMailAccount() != null
                            && mailAccountId.equals(thread.getMailAccount().getId())
                            && gmailThreadId.equals(thread.getGmailThreadId())
                            && direction == thread.getDirection())
                    .findFirst();
        }

        @Override
        public Slice<Thread> findInboxByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Slice<Thread> findSentByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countUnreadInboxByUserId(UUID userId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryMessageRepository implements MessageRepositoryPort {
        private final List<Message> savedMessages = new ArrayList<>();

        @Override
        public Message save(Message message) {
            if (!savedMessages.contains(message)) {
                savedMessages.add(message);
            }
            return message;
        }

        @Override
        public Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId) {
            return savedMessages.stream()
                    .filter(message -> message.getThread() != null
                            && gmailMessageId.equals(message.getGmailMessageId())
                            && (threadId == null || threadId.equals(message.getThread().getId())))
                    .findFirst();
        }

        @Override
        public List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId) {
            return List.of();
        }

        @Override
        public List<Message> findAllByThreadIdInAndDeletedAtIsNull(List<UUID> threadIds) {
            return List.of();
        }

        @Override
        public List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
            return List.of();
        }
    }
}
