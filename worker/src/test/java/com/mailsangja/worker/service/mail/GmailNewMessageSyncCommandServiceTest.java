package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.GoogleMailApiContext;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import com.mailsangja.worker.dto.mail.sync.NewMessageApplyResult;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import com.mailsangja.worker.service.google.GmailMessageApiService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailNewMessageSyncCommandServiceTest {

    @Mock
    private GmailMessageApiService gmailMessageApiService;

    @Mock
    private GmailNewMessageApplyCommandService gmailNewMessageApplyCommandService;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    private GmailNewMessageSyncCommandService service;

    @BeforeEach
    void setUp() {
        service = new GmailNewMessageSyncCommandService(
                gmailMessageApiService,
                gmailNewMessageApplyCommandService,
                messageRepositoryPort
        );
    }

    @Test
    void syncNewMessage_새메시지이면GmailThread를저장하고PushContext를반환한다() {
        UUID mailAccountId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId, "message-new", "thread-1");
        InitialMailSyncThreadResult threadResult = createThreadResult("thread-1", "message-new", Direction.INBOUND);

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccountId, "thread-1", "message-new"
        )).thenReturn(Optional.empty());
        when(gmailMessageApiService.getThreads(new GoogleMailApiContext("access-token", "alice@example.com"), List.of("thread-1")))
                .thenReturn(List.of(threadResult));
        when(gmailNewMessageApplyCommandService.applyNewMessageSync(any(), any(), any()))
                .thenReturn(3);
        when(gmailNewMessageApplyCommandService.findNewMessageApplyResult(
                mailAccountId, "thread-1", "message-new", 3
        )).thenReturn(new NewMessageApplyResult(messageId, threadId, 3));

        Optional<NewMailPushContext> result = service.syncNewMessage(mailAccount, event);

        assertTrue(result.isPresent());
        assertEquals(mailAccountId, result.get().mailAccountId());
        assertEquals("Alice", result.get().alias());
        assertEquals("new subject", result.get().subject());
        assertEquals("new snippet", result.get().snippet());
        assertEquals(threadId, result.get().threadId());
        assertEquals(messageId, result.get().messageId());
        assertEquals(Direction.INBOUND, result.get().direction());
        assertEquals(3, result.get().threadMessageCount());

        ArgumentCaptor<InitialMailSyncThreadSaveCommand> commandCaptor =
                ArgumentCaptor.forClass(InitialMailSyncThreadSaveCommand.class);
        verify(gmailNewMessageApplyCommandService).applyNewMessageSync(
                eq(mailAccount),
                eq(event),
                commandCaptor.capture()
        );
        assertEquals("message-new", commandCaptor.getValue().messages().getFirst().gmailMessageId());
    }

    @Test
    void syncNewMessage_이미저장된메시지이면동기화만하고PushContext는반환하지않는다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId, "message-old", "thread-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccountId, "thread-1", "message-old"
        )).thenReturn(Optional.of(com.mailsangja.db.entity.mail.Message.builder().build()));
        when(gmailMessageApiService.getThreads(new GoogleMailApiContext("access-token", "alice@example.com"), List.of("thread-1")))
                .thenReturn(List.of(createThreadResult("thread-1", "message-old", Direction.INBOUND)));
        when(gmailNewMessageApplyCommandService.applyNewMessageSync(any(), any(), any()))
                .thenReturn(1);

        Optional<NewMailPushContext> result = service.syncNewMessage(mailAccount, event);

        assertTrue(result.isEmpty());
        verify(gmailNewMessageApplyCommandService, never()).findNewMessageApplyResult(any(), any(), any(), anyInt());
    }

    @Test
    void syncNewMessage_GmailThread조회결과가비어있으면예외를던진다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId, "message-1", "thread-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccountId, "thread-1", "message-1"
        )).thenReturn(Optional.empty());
        when(gmailMessageApiService.getThreads(new GoogleMailApiContext("access-token", "alice@example.com"), List.of("thread-1"))).thenReturn(List.of());

        MailPushException exception = assertThrows(MailPushException.class, () -> service.syncNewMessage(mailAccount, event));

        assertEquals(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID, exception.getErrorCode());
        verify(gmailNewMessageApplyCommandService, never()).applyNewMessageSync(any(), any(), any());
    }

    @Test
    void syncNewMessage_스냅샷에이벤트메시지가없으면PushContext를반환하지않는다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId, "message-new", "thread-1");

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccountId, "thread-1", "message-new"
        )).thenReturn(Optional.empty());
        when(gmailMessageApiService.getThreads(new GoogleMailApiContext("access-token", "alice@example.com"), List.of("thread-1")))
                .thenReturn(List.of(createThreadResult("thread-1", "message-other", Direction.INBOUND)));
        when(gmailNewMessageApplyCommandService.applyNewMessageSync(any(), any(), any()))
                .thenReturn(1);
        when(gmailNewMessageApplyCommandService.findNewMessageApplyResult(any(), any(), any(), anyInt()))
                .thenReturn(new NewMessageApplyResult(UUID.randomUUID(), UUID.randomUUID(), 1));

        Optional<NewMailPushContext> result = service.syncNewMessage(mailAccount, event);

        assertTrue(result.isEmpty());
    }

    private MailAccount createMailAccount(UUID mailAccountId) {
        return MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("alice@example.com")
                .alias("Alice")
                .accessToken("access-token")
                .active(true)
                .build();
    }

    private GmailHistoryEvent createEvent(UUID mailAccountId, String gmailMessageId, String gmailThreadId) {
        return new GmailHistoryEvent(
                GmailHistoryEventType.MESSAGE_ADDED,
                mailAccountId,
                gmailMessageId,
                gmailThreadId,
                "history-1"
        );
    }

    private InitialMailSyncThreadResult createThreadResult(String gmailThreadId, String gmailMessageId, Direction direction) {
        return new InitialMailSyncThreadResult(
                gmailThreadId,
                "history-1",
                List.of(new InitialMailSyncMessageResult(
                        gmailMessageId,
                        gmailThreadId,
                        "history-1",
                        null,
                        null,
                        null,
                        null,
                        null,
                        direction,
                        "new subject",
                        "sender@example.com",
                        "Sender",
                        List.of("alice@example.com"),
                        List.of("Alice"),
                        List.of(),
                        List.of(),
                        "new snippet",
                        false,
                        LocalDateTime.of(2026, 5, 22, 10, 0),
                        "body",
                        "<p>body</p>",
                        List.of()
                ))
        );
    }
}
