package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageSaveCommand;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialMailSyncCommandServiceTest {

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    private InitialMailSyncCommandService service;

    @BeforeEach
    void setUp() {
        service = new InitialMailSyncCommandService(threadRepositoryPort, messageRepositoryPort);
    }

    @Test
    void saveThreadBatch_doesNotIncreaseMessageCountForUpdatedMessages() {
        MailAccount mailAccount = createMailAccount();
        Thread thread = createThread(mailAccount, "thread-1", Direction.INBOUND);
        AtomicReference<Message> storedMessage = new AtomicReference<>();

        when(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", Direction.INBOUND
        )).thenReturn(Optional.of(thread));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                .thenAnswer(invocation -> Optional.ofNullable(storedMessage.get()));
        when(messageRepositoryPort.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            storedMessage.set(message);
            return message;
        });

        service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-1",
                java.util.List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-1",
                        null,
                        null,
                        null,
                        null,
                        Direction.INBOUND,
                        "subject",
                        "alice@example.com",
                        "Alice",
                        java.util.List.of("bob@example.com"),
                        java.util.List.of("Bob"),
                        java.util.List.of(),
                        java.util.List.of(),
                        "snippet",
                        false,
                        LocalDateTime.of(2026, 4, 11, 10, 0),
                        "body",
                        null,
                        java.util.List.of()
                ))
        )));

        service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-2",
                java.util.List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-2",
                        null,
                        null,
                        null,
                        null,
                        Direction.INBOUND,
                        "updated subject",
                        "alice@example.com",
                        "Alice",
                        java.util.List.of("bob@example.com"),
                        java.util.List.of("Bob"),
                        java.util.List.of(),
                        java.util.List.of(),
                        "updated snippet",
                        true,
                        LocalDateTime.of(2026, 4, 11, 11, 0),
                        "updated body",
                        null,
                        java.util.List.of()
                ))
        )));

        assertEquals(1, thread.getMessageCount());
        assertEquals("updated subject", thread.getLatestSubject());
        assertEquals("updated snippet", thread.getLatestSnippet());
        verify(messageRepositoryPort, times(1)).save(any(Message.class));
    }

    @Test
    void saveThreadBatch_doesNotReplaceLatestMessageWhenSentAtIsNull() {
        MailAccount mailAccount = createMailAccount();
        Thread thread = createThread(mailAccount, "thread-1", Direction.INBOUND);
        AtomicReference<Message> firstMessage = new AtomicReference<>();

        when(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", Direction.INBOUND
        )).thenReturn(Optional.of(thread));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                .thenAnswer(invocation -> Optional.ofNullable(firstMessage.get()));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-2"))
                .thenReturn(Optional.empty());
        when(messageRepositoryPort.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            if ("message-1".equals(message.getGmailMessageId())) {
                firstMessage.set(message);
            }
            return message;
        });

        service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-1",
                java.util.List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-1",
                        null,
                        null,
                        null,
                        null,
                        Direction.INBOUND,
                        "subject-1",
                        "alice@example.com",
                        "Alice",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        "snippet-1",
                        false,
                        LocalDateTime.of(2026, 4, 11, 10, 0),
                        null,
                        null,
                        java.util.List.of()
                ))
        )));

        service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-2",
                java.util.List.of(new InitialMailSyncMessageSaveCommand(
                        "message-2",
                        "history-2",
                        null,
                        null,
                        null,
                        null,
                        Direction.INBOUND,
                        "subject-2",
                        "carol@example.com",
                        "Carol",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        "snippet-2",
                        true,
                        null,
                        null,
                        null,
                        java.util.List.of()
                ))
        )));

        assertEquals("subject-1", thread.getLatestSubject());
        assertEquals("snippet-1", thread.getLatestSnippet());
        assertEquals(LocalDateTime.of(2026, 4, 11, 10, 0), thread.getLastMessageAt());
        assertEquals(2, thread.getMessageCount());
    }

    @Test
    void saveThreadBatch_fillsMessageNamesAndLatestParticipantNameWithAddressFallback() {
        MailAccount mailAccount = createMailAccount();
        Thread thread = createThread(mailAccount, "thread-1", Direction.OUTBOUND);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        when(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", Direction.OUTBOUND
        )).thenReturn(Optional.of(thread));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                .thenReturn(Optional.empty());
        when(messageRepositoryPort.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-1",
                java.util.List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-1",
                        null,
                        null,
                        null,
                        null,
                        Direction.OUTBOUND,
                        "subject",
                        "sender@example.com",
                        null,
                        java.util.List.of("first@example.com", "second@example.com"),
                        Arrays.asList((String) null),
                        java.util.List.of("cc@example.com"),
                        java.util.List.of(""),
                        "snippet",
                        true,
                        LocalDateTime.of(2026, 4, 11, 10, 0),
                        "body",
                        null,
                        java.util.List.of()
                ))
        )));

        verify(messageRepositoryPort).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();
        assertEquals("sender@example.com", savedMessage.getFromName());
        assertEquals(java.util.List.of("first@example.com", "second@example.com"), savedMessage.getToNames());
        assertEquals(java.util.List.of("cc@example.com"), savedMessage.getCcNames());
        assertEquals("first@example.com", thread.getLatestParticipantAddress());
        assertEquals("first@example.com", thread.getLatestParticipantName());
    }

    @Test
    void saveThreadBatch_trimsNamesBeforeSaving() {
        MailAccount mailAccount = createMailAccount();
        Thread thread = createThread(mailAccount, "thread-1", Direction.OUTBOUND);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        when(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", Direction.OUTBOUND
        )).thenReturn(Optional.of(thread));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                .thenReturn(Optional.empty());
        when(messageRepositoryPort.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-1",
                java.util.List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-1",
                        null,
                        null,
                        null,
                        null,
                        Direction.OUTBOUND,
                        "subject",
                        "sender@example.com",
                        "  Sender Name  ",
                        java.util.List.of("first@example.com"),
                        java.util.List.of("  First Receiver  "),
                        java.util.List.of("cc@example.com"),
                        java.util.List.of("  "),
                        "snippet",
                        true,
                        LocalDateTime.of(2026, 4, 11, 10, 0),
                        "body",
                        null,
                        java.util.List.of()
                ))
        )));

        verify(messageRepositoryPort).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();
        assertEquals("Sender Name", savedMessage.getFromName());
        assertEquals(java.util.List.of("First Receiver"), savedMessage.getToNames());
        assertEquals(java.util.List.of("cc@example.com"), savedMessage.getCcNames());
        assertEquals("First Receiver", thread.getLatestParticipantName());
    }

    @Test
    void saveThreadBatch_usesCcAsLatestParticipantWhenToIsEmpty() {
        MailAccount mailAccount = createMailAccount();
        Thread thread = createThread(mailAccount, "thread-1", Direction.OUTBOUND);

        when(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", Direction.OUTBOUND
        )).thenReturn(Optional.of(thread));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                .thenReturn(Optional.empty());
        when(messageRepositoryPort.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                "thread-1",
                "history-1",
                java.util.List.of(new InitialMailSyncMessageSaveCommand(
                        "message-1",
                        "history-1",
                        null,
                        null,
                        null,
                        null,
                        Direction.OUTBOUND,
                        "subject",
                        "sender@example.com",
                        "Sender",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of("cc@example.com"),
                        java.util.List.of("CC Receiver"),
                        "snippet",
                        true,
                        LocalDateTime.of(2026, 4, 11, 10, 0),
                        "body",
                        null,
                        java.util.List.of()
                ))
        )));

        assertEquals("cc@example.com", thread.getLatestParticipantAddress());
        assertEquals("CC Receiver", thread.getLatestParticipantName());
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
    }

    private Thread createThread(MailAccount mailAccount, String gmailThreadId, Direction direction) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(direction)
                .read(true)
                .messageCount(0)
                .build();
    }
}
