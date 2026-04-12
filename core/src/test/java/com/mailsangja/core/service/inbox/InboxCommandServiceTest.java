package com.mailsangja.core.service.inbox;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InboxCommandServiceTest {

    @Test
    void markThreadAsRead_같은지메일스레드의양방향스레드와메시지를모두읽음처리한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-1";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread inboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.INBOUND)
                .read(false)
                .build();
        Thread outboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.OUTBOUND)
                .read(false)
                .build();
        Message inboundMessage = Message.builder()
                .thread(inboundThread)
                .gmailMessageId("gmail-message-1")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
        Message outboundMessage = Message.builder()
                .thread(outboundThread)
                .gmailMessageId("gmail-message-2")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();

        InboxCommandService inboxCommandService = new InboxCommandService(
                new FakeMessageRepositoryPort(mailAccountId, gmailThreadId, List.of(inboundMessage, outboundMessage)),
                new FakeThreadRepositoryPort(mailAccountId, gmailThreadId, List.of(inboundThread, outboundThread))
        );

        inboxCommandService.markThreadAsRead(inboundThread);

        assertTrue(inboundThread.isRead());
        assertTrue(outboundThread.isRead());
        assertTrue(inboundMessage.isRead());
        assertTrue(outboundMessage.isRead());
    }

    @Test
    void markThreadAsRead_이미읽은메시지에도안전하게동작한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-2";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.OUTBOUND)
                .read(true)
                .build();
        Message readMessage = Message.builder()
                .thread(thread)
                .gmailMessageId("gmail-message-3")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(true)
                .build();

        InboxCommandService inboxCommandService = new InboxCommandService(
                new FakeMessageRepositoryPort(mailAccountId, gmailThreadId, List.of(readMessage)),
                new FakeThreadRepositoryPort(mailAccountId, gmailThreadId, List.of(thread))
        );

        inboxCommandService.markThreadAsRead(thread);

        assertTrue(thread.isRead());
        assertTrue(readMessage.isRead());
    }

    private static class FakeMessageRepositoryPort implements MessageRepositoryPort {

        private final UUID expectedMailAccountId;
        private final String expectedGmailThreadId;
        private final List<Message> messages;

        private FakeMessageRepositoryPort(UUID expectedMailAccountId, String expectedGmailThreadId, List<Message> messages) {
            this.expectedMailAccountId = expectedMailAccountId;
            this.expectedGmailThreadId = expectedGmailThreadId;
            this.messages = new ArrayList<>(messages);
        }

        @Override
        public Message save(Message message) {
            return message;
        }

        @Override
        public Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId) {
            return Optional.empty();
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
            if (!expectedMailAccountId.equals(mailAccountId) || !expectedGmailThreadId.equals(gmailThreadId)) {
                return List.of();
            }
            return List.copyOf(messages);
        }
    }

    private static class FakeThreadRepositoryPort implements ThreadRepositoryPort {

        private final UUID expectedMailAccountId;
        private final String expectedGmailThreadId;
        private final List<Thread> threads;

        private FakeThreadRepositoryPort(UUID expectedMailAccountId, String expectedGmailThreadId, List<Thread> threads) {
            this.expectedMailAccountId = expectedMailAccountId;
            this.expectedGmailThreadId = expectedGmailThreadId;
            this.threads = new ArrayList<>(threads);
        }

        @Override
        public Thread save(Thread thread) {
            return thread;
        }

        @Override
        public Optional<Thread> findByIdAndDeletedAtIsNull(UUID id) {
            return threads.stream().filter(thread -> id.equals(thread.getId())).findFirst();
        }

        @Override
        public Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                UUID mailAccountId,
                String gmailThreadId,
                Direction direction
        ) {
            return threads.stream()
                    .filter(thread -> mailAccountId.equals(thread.getMailAccount().getId()))
                    .filter(thread -> gmailThreadId.equals(thread.getGmailThreadId()))
                    .filter(thread -> direction == thread.getDirection())
                    .findFirst();
        }

        @Override
        public List<Thread> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
            if (!expectedMailAccountId.equals(mailAccountId) || !expectedGmailThreadId.equals(gmailThreadId)) {
                return List.of();
            }
            return List.copyOf(threads);
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
}
