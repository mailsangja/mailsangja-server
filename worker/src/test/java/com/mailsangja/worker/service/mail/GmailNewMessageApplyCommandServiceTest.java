package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.GmailThreadLock;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncAttachmentResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageSaveCommand;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailNewMessageApplyCommandServiceTest {

    @Test
    void applyNewMessageSync_whenThreadIsDeleted_restoresOnlyThreadAndInsertsNewMessage() {
        InMemoryThreadRepository threadRepository = new InMemoryThreadRepository();
        InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        InitialMailSyncCommandService initialMailSyncCommandService =
                new InitialMailSyncCommandService(threadRepository, messageRepository);
        GmailNewMessageApplyCommandService service = new GmailNewMessageApplyCommandService(
                new NoOpLockRepository(), threadRepository, initialMailSyncCommandService);

        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();

        Thread deletedThread = Thread.builder()
                .mailAccount(mailAccount)
                .gmailThreadId("thread-1")
                .direction(Direction.INBOUND)
                .read(false)
                .messageCount(1)
                .build();
        deletedThread.delete();
        threadRepository.savedThreads.add(deletedThread);

        Message oldMessage = Message.from(deletedThread, new Message.CreateValues(
                "message-old", Direction.INBOUND, "old subject", "sender@example.com",
                List.of("me@example.com"), List.of(), "old snippet", false,
                LocalDateTime.of(2026, 4, 10, 9, 0), null, null));
        oldMessage.delete();
        messageRepository.savedMessages.add(oldMessage);

        GmailHistoryEvent event = new GmailHistoryEvent(
                GmailHistoryEventType.MESSAGE_ADDED,
                mailAccount.getId(),
                "message-new",
                "thread-1",
                "history-2"
        );

        InitialMailSyncThreadSaveCommand syncCommand = new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-2",
                List.of(
                        new InitialMailSyncMessageSaveCommand(
                                "message-old", "history-1", Direction.INBOUND,
                                "old subject", "sender@example.com", List.of("me@example.com"),
                                List.of(), "old snippet", false,
                                LocalDateTime.of(2026, 4, 10, 9, 0), null, null, List.of()),
                        new InitialMailSyncMessageSaveCommand(
                                "message-new", "history-2", Direction.INBOUND,
                                "new subject", "sender@example.com", List.of("me@example.com"),
                                List.of(), "new snippet", false,
                                LocalDateTime.of(2026, 4, 14, 10, 0), null, null, List.of())
                )
        );

        service.applyNewMessageSync(mailAccount, event, syncCommand);

        // Thread는 복구되어야 한다
        assertFalse(deletedThread.isDeleted(), "Thread should be restored");

        // 기존 message는 여전히 삭제 상태여야 한다
        assertTrue(oldMessage.isDeleted(), "Old message should remain soft-deleted");

        // 신규 메시지만 삽입되어야 한다
        long activeMessages = messageRepository.savedMessages.stream()
                .filter(m -> !m.isDeleted())
                .count();
        assertEquals(1, activeMessages, "Only the new message should be active");

        Message insertedMessage = messageRepository.savedMessages.stream()
                .filter(m -> !m.isDeleted())
                .findFirst()
                .orElseThrow();
        assertEquals("message-new", insertedMessage.getGmailMessageId());

        // Thread의 messageCount는 신규 메시지 1건만 반영해야 한다
        assertEquals(1, deletedThread.getMessageCount());

        // Thread의 최신 정보는 신규 메시지로 업데이트되어야 한다
        assertEquals("new subject", deletedThread.getLatestSubject());
        assertEquals("new snippet", deletedThread.getLatestSnippet());
        assertEquals(LocalDateTime.of(2026, 4, 14, 10, 0), deletedThread.getLastMessageAt());
    }

    @Test
    void applyNewMessageSync_whenThreadIsNotDeleted_insertsNewMessageAndUpdatesThreadInfo() {
        InMemoryThreadRepository threadRepository = new InMemoryThreadRepository();
        InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        InitialMailSyncCommandService initialMailSyncCommandService =
                new InitialMailSyncCommandService(threadRepository, messageRepository);
        GmailNewMessageApplyCommandService service = new GmailNewMessageApplyCommandService(
                new NoOpLockRepository(), threadRepository, initialMailSyncCommandService);

        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();

        Thread activeThread = Thread.builder()
                .mailAccount(mailAccount)
                .gmailThreadId("thread-2")
                .direction(Direction.INBOUND)
                .read(false)
                .messageCount(1)
                .build();
        threadRepository.savedThreads.add(activeThread);

        Message existingMessage = Message.from(activeThread, new Message.CreateValues(
                "message-old", Direction.INBOUND, "old subject", "sender@example.com",
                List.of("me@example.com"), List.of(), "old snippet", false,
                LocalDateTime.of(2026, 4, 10, 9, 0), null, null));
        messageRepository.savedMessages.add(existingMessage);

        GmailHistoryEvent event = new GmailHistoryEvent(
                GmailHistoryEventType.MESSAGE_ADDED,
                mailAccount.getId(),
                "message-new",
                "thread-2",
                "history-2"
        );

        InitialMailSyncThreadSaveCommand syncCommand = new InitialMailSyncThreadSaveCommand(
                "thread-2",
                "history-2",
                List.of(
                        new InitialMailSyncMessageSaveCommand(
                                "message-old", "history-1", Direction.INBOUND,
                                "old subject", "sender@example.com", List.of("me@example.com"),
                                List.of(), "old snippet", false,
                                LocalDateTime.of(2026, 4, 10, 9, 0), null, null, List.of()),
                        new InitialMailSyncMessageSaveCommand(
                                "message-new", "history-2", Direction.INBOUND,
                                "new subject", "sender@example.com", List.of("me@example.com"),
                                List.of(), "new snippet", false,
                                LocalDateTime.of(2026, 4, 14, 10, 0), null, null, List.of())
                )
        );

        service.applyNewMessageSync(mailAccount, event, syncCommand);

        assertFalse(activeThread.isDeleted());
        assertEquals(2, activeThread.getMessageCount());
        assertEquals("new subject", activeThread.getLatestSubject());
        assertEquals(LocalDateTime.of(2026, 4, 14, 10, 0), activeThread.getLastMessageAt());
    }

    private static final class NoOpLockRepository implements GmailThreadLockRepositoryPort {
        @Override
        public GmailThreadLock save(GmailThreadLock lock) {
            return lock;
        }

        @Override
        public void acquireThreadLock(MailAccount mailAccount, String gmailThreadId) {
        }

        @Override
        public Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
            return Optional.empty();
        }

        @Override
        public Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(UUID mailAccountId, String gmailThreadId) {
            return Optional.empty();
        }
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
            return Optional.empty();
        }

        @Override
        public Optional<Thread> findByIdIncludingDeleted(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                UUID mailAccountId, String gmailThreadId, Direction direction) {
            return savedThreads.stream()
                    .filter(t -> t.getMailAccount() != null
                            && mailAccountId.equals(t.getMailAccount().getId())
                            && gmailThreadId.equals(t.getGmailThreadId())
                            && direction == t.getDirection()
                            && !t.isDeleted())
                    .findFirst();
        }

        @Override
        public List<Thread> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
            return savedThreads.stream()
                    .filter(t -> t.getMailAccount() != null
                            && mailAccountId.equals(t.getMailAccount().getId())
                            && gmailThreadId.equals(t.getGmailThreadId())
                            && !t.isDeleted())
                    .toList();
        }

        @Override
        public List<Thread> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return savedThreads.stream()
                    .filter(t -> t.getMailAccount() != null
                            && mailAccountId.equals(t.getMailAccount().getId())
                            && gmailThreadId.equals(t.getGmailThreadId()))
                    .toList();
        }

        @Override
        public int bulkSoftDeleteByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId, LocalDateTime deletedAt) {
            return 0;
        }

        @Override
        public int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return 0;
        }

        @Override
        public int bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            List<Thread> targets = savedThreads.stream()
                    .filter(t -> t.getMailAccount() != null
                            && mailAccountId.equals(t.getMailAccount().getId())
                            && gmailThreadId.equals(t.getGmailThreadId())
                            && t.isDeleted())
                    .toList();
            targets.forEach(t -> {
                t.restore();
                t.updateMessageCount(0);
            });
            return targets.size();
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

        @Override
        public Slice<Thread> findTrashByUserId(UUID userId, UUID markerId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void hardDeleteAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
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
                    .filter(m -> m.getThread() != null
                            && gmailMessageId.equals(m.getGmailMessageId())
                            && (threadId == null || threadId.equals(m.getThread().getId()))
                            && !m.isDeleted())
                    .findFirst();
        }

        @Override
        public Optional<Message> findByThreadIdAndGmailMessageId(UUID threadId, String gmailMessageId) {
            return savedMessages.stream()
                    .filter(m -> m.getThread() != null
                            && gmailMessageId.equals(m.getGmailMessageId())
                            && (threadId == null || threadId.equals(m.getThread().getId())))
                    .findFirst();
        }

        @Override
        public Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                UUID mailAccountId, String gmailThreadId, String gmailMessageId) {
            return Optional.empty();
        }

        @Override
        public Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                UUID mailAccountId, String gmailThreadId, String gmailMessageId) {
            return Optional.empty();
        }

        @Override
        public boolean existsByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndGmailMessageIdNot(
                UUID mailAccountId, String gmailThreadId, String gmailMessageId) {
            return false;
        }

        @Override
        public Optional<Message> findByIdIncludingDeleted(UUID messageId) {
            return Optional.empty();
        }

        @Override
        public List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId) {
            return List.of();
        }

        @Override
        public List<Message> findAllByThreadIdIncludingDeleted(UUID threadId) {
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

        @Override
        public List<Message> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return List.of();
        }

        @Override
        public Slice<Message> findDeletedByUserId(UUID userId, UUID markerId, Pageable pageable) {
            return new SliceImpl<>(List.of());
        }

        @Override
        public List<Message> findAllDeletedByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return List.of();
        }

        @Override
        public int bulkSoftDeleteByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId, LocalDateTime deletedAt) {
            return 0;
        }

        @Override
        public int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return 0;
        }

        @Override
        public void hardDelete(Message message) {
        }

        @Override
        public boolean existsByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return false;
        }
    }
}
