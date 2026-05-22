package com.mailsangja.worker.service.trash;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailHistoryDeleteApplyCommandServiceTest {

    @Mock private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;
    @Mock private ThreadRepositoryPort threadRepositoryPort;
    @Mock private MessageRepositoryPort messageRepositoryPort;

    private GmailHistoryDeleteApplyCommandService service;

    @BeforeEach
    void setUp() {
        service = new GmailHistoryDeleteApplyCommandService(
                gmailThreadLockRepositoryPort,
                threadRepositoryPort,
                messageRepositoryPort
        );
    }

    @Test
    void applyMessageTrashed_대상메시지가없으면락만잡고종료한다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_TRASHED, "message-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", "message-1"
        )).thenReturn(Optional.empty());

        service.applyMessageTrashed(mailAccount, event);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, "thread-1");
        verify(threadRepositoryPort, never()).bulkSoftDeleteByMailAccountIdAndGmailThreadId(any(), any(), any());
    }

    @Test
    void applyMessageTrashed_마지막활성메시지를삭제하면스레드를SoftDelete한다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_TRASHED, "message-1");
        Message message = createMessage(createThread(mailAccount, Direction.INBOUND), "message-1", Direction.INBOUND, false,
                LocalDateTime.of(2026, 5, 22, 9, 0));

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", "message-1"
        )).thenReturn(Optional.of(message));
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of());

        service.applyMessageTrashed(mailAccount, event);

        ArgumentCaptor<LocalDateTime> deletedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        assertTrue(message.isDeleted());
        verify(threadRepositoryPort).bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                org.mockito.ArgumentMatchers.eq(mailAccount.getId()),
                org.mockito.ArgumentMatchers.eq("thread-1"),
                deletedAtCaptor.capture()
        );
        assertNotNull(deletedAtCaptor.getValue());
    }

    @Test
    void applyMessageTrashed_남은메시지가있으면스레드최신정보와읽음상태를갱신한다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        Thread inboundThread = createThread(mailAccount, Direction.INBOUND);
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_TRASHED, "message-old");
        Message deleted = createMessage(inboundThread, "message-old", Direction.INBOUND, true,
                LocalDateTime.of(2026, 5, 21, 9, 0));
        Message latest = createMessage(inboundThread, "message-latest", Direction.INBOUND, false,
                LocalDateTime.of(2026, 5, 22, 9, 0));

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", "message-old"
        )).thenReturn(Optional.of(deleted));
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(latest));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(inboundThread));

        service.applyMessageTrashed(mailAccount, event);

        assertTrue(deleted.isDeleted());
        assertEquals("subject-message-latest", inboundThread.getLatestSubject());
        assertEquals("snippet-message-latest", inboundThread.getLatestSnippet());
        assertEquals("sender@example.com", inboundThread.getLatestParticipantAddress());
        assertEquals("Sender", inboundThread.getLatestParticipantName());
        assertEquals(1, inboundThread.getMessageCount());
        assertFalse(inboundThread.isRead());
    }

    @Test
    void applyMessageRestored_삭제메시지를복원하고스레드도복원한다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        Thread thread = createThread(mailAccount, Direction.OUTBOUND);
        thread.delete();
        Message message = createMessage(thread, "message-1", Direction.OUTBOUND, true,
                LocalDateTime.of(2026, 5, 22, 9, 0));
        message.delete();
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_RESTORED, "message-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                mailAccount.getId(), "thread-1", "message-1"
        )).thenReturn(Optional.of(message));
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(message));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(thread));

        service.applyMessageRestored(mailAccount, event);

        assertFalse(message.isDeleted());
        verify(threadRepositoryPort).bulkRestoreByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1");
        assertEquals("to@example.com", thread.getLatestParticipantAddress());
        assertEquals("To", thread.getLatestParticipantName());
        assertTrue(thread.isRead());
    }

    @Test
    void applyMessageRestored_대상메시지가없거나삭제상태가아니면복원하지않는다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        Message activeMessage = createMessage(createThread(mailAccount, Direction.INBOUND), "message-1", Direction.INBOUND, true,
                LocalDateTime.of(2026, 5, 22, 9, 0));
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_RESTORED, "message-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                mailAccount.getId(), "thread-1", "message-1"
        )).thenReturn(Optional.of(activeMessage));

        service.applyMessageRestored(mailAccount, event);

        verify(threadRepositoryPort, never()).bulkRestoreByMailAccountIdAndGmailThreadId(any(), any());
    }

    @Test
    void applyMessagePermanentlyDeleted_마지막메시지이면메시지와스레드를HardDelete한다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        Message message = createMessage(createThread(mailAccount, Direction.INBOUND), "message-1", Direction.INBOUND, true,
                LocalDateTime.of(2026, 5, 22, 9, 0));
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_PERMANENTLY_DELETED, "message-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                mailAccount.getId(), "thread-1", "message-1"
        )).thenReturn(Optional.of(message));
        when(messageRepositoryPort.existsByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1"))
                .thenReturn(false);

        service.applyMessagePermanentlyDeleted(mailAccount, event);

        verify(messageRepositoryPort).hardDelete(message);
        verify(threadRepositoryPort).hardDeleteAllByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1");
    }

    @Test
    void applyMessagePermanentlyDeleted_남은메시지가있으면스레드는삭제하지않는다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        Message message = createMessage(createThread(mailAccount, Direction.INBOUND), "message-1", Direction.INBOUND, true,
                LocalDateTime.of(2026, 5, 22, 9, 0));
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_PERMANENTLY_DELETED, "message-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                mailAccount.getId(), "thread-1", "message-1"
        )).thenReturn(Optional.of(message));
        when(messageRepositoryPort.existsByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1"))
                .thenReturn(true);

        service.applyMessagePermanentlyDeleted(mailAccount, event);

        verify(messageRepositoryPort).hardDelete(message);
        verify(threadRepositoryPort, never()).hardDeleteAllByMailAccountIdAndGmailThreadId(any(), any());
    }

    @Test
    void applyMessagePermanentlyDeleted_대상메시지가없으면HardDelete하지않는다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_PERMANENTLY_DELETED, "message-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                mailAccount.getId(), "thread-1", "message-1"
        )).thenReturn(Optional.empty());

        service.applyMessagePermanentlyDeleted(mailAccount, event);

        verify(messageRepositoryPort, never()).hardDelete(any());
        verify(threadRepositoryPort, never()).hardDeleteAllByMailAccountIdAndGmailThreadId(any(), any());
    }

    private GmailHistoryEvent createEvent(UUID mailAccountId, GmailHistoryEventType eventType, String gmailMessageId) {
        return new GmailHistoryEvent(eventType, mailAccountId, gmailMessageId, "thread-1", "history-1");
    }

    private Thread createThread(MailAccount mailAccount, Direction direction) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("thread-1")
                .direction(direction)
                .read(true)
                .messageCount(2)
                .build();
    }

    private Message createMessage(Thread thread, String gmailMessageId, Direction direction, boolean read, LocalDateTime sentAt) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(gmailMessageId)
                .direction(direction)
                .subject("subject-" + gmailMessageId)
                .fromAddress("sender@example.com")
                .fromName("Sender")
                .toAddresses(List.of("to@example.com"))
                .toNames(List.of("To"))
                .ccAddresses(List.of())
                .ccNames(List.of())
                .snippet("snippet-" + gmailMessageId)
                .read(read)
                .sentAt(sentAt)
                .build();
    }
}
