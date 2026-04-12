package com.mailsangja.core.service.inbox;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InboxCommandServiceTest {

    @Test
    void markThreadAsRead_스레드와메시지를모두읽음처리한다() {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .direction(Direction.INBOUND)
                .read(false)
                .build();
        Message firstMessage = Message.builder()
                .thread(thread)
                .gmailMessageId("gmail-message-1")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
        Message secondMessage = Message.builder()
                .thread(thread)
                .gmailMessageId("gmail-message-2")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();

        InboxCommandService inboxCommandService = new InboxCommandService(
                new FakeMessageRepositoryPort(List.of(firstMessage, secondMessage))
        );

        inboxCommandService.markThreadAsRead(thread);

        assertTrue(thread.isRead());
        assertTrue(firstMessage.isRead());
        assertTrue(secondMessage.isRead());
    }

    @Test
    void markThreadAsRead_이미읽은메시지에도안전하게동작한다() {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
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
                new FakeMessageRepositoryPort(List.of(readMessage))
        );

        inboxCommandService.markThreadAsRead(thread);

        assertTrue(thread.isRead());
        assertTrue(readMessage.isRead());
    }

    private static class FakeMessageRepositoryPort implements MessageRepositoryPort {

        private final List<Message> messages;

        private FakeMessageRepositoryPort(List<Message> messages) {
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
            return List.copyOf(messages);
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
