package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailHistoryStateApplyCommandServiceTest {

    @Mock private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;
    @Mock private GmailHistoryStateQueryService gmailHistoryStateQueryService;
    @Mock private ThreadRepositoryPort threadRepositoryPort;
    @Mock private MessageRepositoryPort messageRepositoryPort;
    @Mock private InitialMailSyncCommandService initialMailSyncCommandService;

    private GmailHistoryStateApplyCommandService service;

    @BeforeEach
    void setUp() {
        service = new GmailHistoryStateApplyCommandService(
                gmailThreadLockRepositoryPort,
                gmailHistoryStateQueryService,
                threadRepositoryPort,
                messageRepositoryPort,
                initialMailSyncCommandService
        );
    }

    @Test
    void applyMessageReadState_누락메시지스냅샷이있으면저장후읽음상태를반영한다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_READ, "message-1");
        InitialMailSyncThreadSaveCommand syncCommand = new InitialMailSyncThreadSaveCommand("thread-1", "history-1", List.of());
        Message target = createMessage("message-1", false);
        Message alreadyRead = createMessage("message-2", true);
        Thread thread = createThread(mailAccount, false);

        when(gmailHistoryStateQueryService.existsMessage(mailAccount.getId(), "thread-1", "message-1")).thenReturn(false);
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(target, alreadyRead));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(thread));

        service.applyMessageReadState(mailAccount, event, true, syncCommand);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, "thread-1");
        verify(initialMailSyncCommandService).saveMissingMessagesFromThreadSnapshot(mailAccount, syncCommand);
        assertTrue(target.isRead());
        assertTrue(thread.isRead());
        assertEquals("history-1", thread.getHistoryId());
    }

    @Test
    void applyMessageReadState_안읽음이면스레드도안읽음으로반영한다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_UNREAD, "message-1");
        Message target = createMessage("message-1", true);
        Message another = createMessage("message-2", true);
        Thread thread = createThread(mailAccount, true);

        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(target, another));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(thread));

        service.applyMessageReadState(mailAccount, event, false, null);

        verifyNoInteractions(initialMailSyncCommandService);
        assertFalse(target.isRead());
        assertFalse(thread.isRead());
    }

    @Test
    void applyMessageReadState_대상메시지가없으면예외를던진다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_READ, "missing");

        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(createMessage("message-1", false)));

        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.applyMessageReadState(mailAccount, event, true, null)
        );

        assertEquals(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID, exception.getErrorCode());
    }

    @Test
    void applyMessageReadState_스레드가없으면예외를던진다() {
        MailAccount mailAccount = MailAccount.builder().id(UUID.randomUUID()).build();
        GmailHistoryEvent event = createEvent(mailAccount.getId(), GmailHistoryEventType.MESSAGE_READ, "message-1");

        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of(createMessage("message-1", false)));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                .thenReturn(List.of());

        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.applyMessageReadState(mailAccount, event, true, null)
        );

        assertEquals(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID, exception.getErrorCode());
    }

    private GmailHistoryEvent createEvent(UUID mailAccountId, GmailHistoryEventType eventType, String gmailMessageId) {
        return new GmailHistoryEvent(eventType, mailAccountId, gmailMessageId, "thread-1", "history-1");
    }

    private Thread createThread(MailAccount mailAccount, boolean read) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("thread-1")
                .direction(Direction.INBOUND)
                .read(read)
                .messageCount(2)
                .build();
    }

    private Message createMessage(String gmailMessageId, boolean read) {
        return Message.builder()
                .id(UUID.randomUUID())
                .gmailMessageId(gmailMessageId)
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .subject("subject")
                .snippet("snippet")
                .read(read)
                .sentAt(LocalDateTime.of(2026, 5, 22, 9, 0))
                .build();
    }
}
