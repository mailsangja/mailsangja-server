package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageSaveCommand;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailNewMessageApplyCommandServiceTest {

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    private GmailNewMessageApplyCommandService service;

    @BeforeEach
    void setUp() {
        InitialMailSyncCommandService initialMailSyncCommandService =
                new InitialMailSyncCommandService(threadRepositoryPort, messageRepositoryPort);
        service = new GmailNewMessageApplyCommandService(
                gmailThreadLockRepositoryPort,
                threadRepositoryPort,
                messageRepositoryPort,
                initialMailSyncCommandService
        );
    }

    @Test
    void applyNewMessageSync_whenThreadIsDeleted_restoresOnlyThreadAndInsertsNewMessage() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        Thread deletedThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("thread-1")
                .direction(Direction.INBOUND)
                .read(false)
                .messageCount(1)
                .build();
        deletedThread.delete();

        Message oldMessage = Message.from(deletedThread, new Message.CreateValues(
                "message-old", null, null, null, null, null, Direction.INBOUND, "old subject", "sender@example.com", "Sender",
                List.of("me@example.com"), List.of("Me"), List.of(), List.of(), "old snippet", false,
                LocalDateTime.of(2026, 4, 10, 9, 0), null, null
        ));
        oldMessage.delete();

        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(deletedThread));
        when(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccount.getId(), "thread-1", Direction.INBOUND
        )).thenAnswer(invocation -> deletedThread.isDeleted() ? Optional.empty() : Optional.of(deletedThread));
        when(threadRepositoryPort.bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1"))
                .thenAnswer(invocation -> {
                    deletedThread.restore();
                    deletedThread.updateMessageCount(0);
                    return 1;
                });
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(deletedThread.getId(), "message-old"))
                .thenReturn(Optional.of(oldMessage));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(deletedThread.getId(), "message-new"))
                .thenReturn(Optional.empty());
        when(messageRepositoryPort.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GmailHistoryEvent event = new GmailHistoryEvent(
                GmailHistoryEventType.MESSAGE_ADDED,
                mailAccount.getId(),
                "message-new",
                "thread-1",
                "history-2"
        );
        InitialMailSyncThreadSaveCommand syncCommand = createSyncCommand("thread-1", "history-2", "message-new", "new subject", "new snippet");

        service.applyNewMessageSync(mailAccount, event, syncCommand);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepositoryPort).save(messageCaptor.capture());
        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, "thread-1");
        verify(threadRepositoryPort).bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1");
        assertFalse(deletedThread.isDeleted());
        assertTrue(oldMessage.isDeleted());
        assertEquals("message-new", messageCaptor.getValue().getGmailMessageId());
        assertEquals(1, deletedThread.getMessageCount());
        assertEquals("new subject", deletedThread.getLatestSubject());
        assertEquals("new snippet", deletedThread.getLatestSnippet());
        assertEquals(LocalDateTime.of(2026, 4, 14, 10, 0), deletedThread.getLastMessageAt());
    }

    @Test
    void applyNewMessageSync_whenThreadIsNotDeleted_insertsNewMessageAndUpdatesThreadInfo() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        Thread activeThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("thread-2")
                .direction(Direction.INBOUND)
                .read(false)
                .messageCount(1)
                .build();

        Message existingMessage = Message.from(activeThread, new Message.CreateValues(
                "message-old", null, null, null, null, null, Direction.INBOUND, "old subject", "sender@example.com", "Sender",
                List.of("me@example.com"), List.of("Me"), List.of(), List.of(), "old snippet", false,
                LocalDateTime.of(2026, 4, 10, 9, 0), null, null
        ));

        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-2"))
                .thenReturn(List.of(activeThread));
        when(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccount.getId(), "thread-2", Direction.INBOUND
        )).thenReturn(Optional.of(activeThread));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(activeThread.getId(), "message-old"))
                .thenReturn(Optional.of(existingMessage));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageId(activeThread.getId(), "message-new"))
                .thenReturn(Optional.empty());
        when(messageRepositoryPort.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GmailHistoryEvent event = new GmailHistoryEvent(
                GmailHistoryEventType.MESSAGE_ADDED,
                mailAccount.getId(),
                "message-new",
                "thread-2",
                "history-2"
        );
        InitialMailSyncThreadSaveCommand syncCommand = createSyncCommand("thread-2", "history-2", "message-new", "new subject", "new snippet");

        service.applyNewMessageSync(mailAccount, event, syncCommand);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, "thread-2");
        verify(threadRepositoryPort, never()).bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(any(), any());
        assertFalse(activeThread.isDeleted());
        assertEquals(2, activeThread.getMessageCount());
        assertEquals("new subject", activeThread.getLatestSubject());
        assertEquals(LocalDateTime.of(2026, 4, 14, 10, 0), activeThread.getLastMessageAt());
    }

    private InitialMailSyncThreadSaveCommand createSyncCommand(
            String gmailThreadId,
            String historyId,
            String newMessageId,
            String newSubject,
            String newSnippet
    ) {
        return new InitialMailSyncThreadSaveCommand(
                gmailThreadId,
                historyId,
                List.of(
                        new InitialMailSyncMessageSaveCommand(
                                "message-old", "history-1", null, null, null, null, null, Direction.INBOUND,
                                "old subject", "sender@example.com", "Sender", List.of("me@example.com"),
                                List.of("Me"), List.of(), List.of(), "old snippet", false,
                                LocalDateTime.of(2026, 4, 10, 9, 0), null, null, List.of()
                        ),
                        new InitialMailSyncMessageSaveCommand(
                                newMessageId, historyId, null, null, null, null, null, Direction.INBOUND,
                                newSubject, "sender@example.com", "Sender", List.of("me@example.com"),
                                List.of("Me"), List.of(), List.of(), newSnippet, false,
                                LocalDateTime.of(2026, 4, 14, 10, 0), null, null, List.of()
                        )
                )
        );
    }
}
