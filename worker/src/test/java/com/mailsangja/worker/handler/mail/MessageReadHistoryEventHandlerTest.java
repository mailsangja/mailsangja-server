package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import com.mailsangja.worker.service.google.GmailMessageApiService;
import com.mailsangja.worker.service.mail.GmailHistoryStateApplyCommandService;
import com.mailsangja.worker.service.mail.GmailHistoryStateQueryService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MessageReadHistoryEventHandlerTest {

    @Mock private MailAccountQueryService mailAccountQueryService;
    @Mock private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    @Mock private GmailHistoryStateQueryService gmailHistoryStateQueryService;
    @Mock private GmailMessageApiService gmailMessageApiService;
    @Mock private GmailHistoryStateApplyCommandService gmailHistoryStateApplyCommandService;

    private MessageReadHistoryEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MessageReadHistoryEventHandler(
                mailAccountQueryService,
                googleAccessTokenEnsureService,
                gmailHistoryStateQueryService,
                gmailMessageApiService,
                gmailHistoryStateApplyCommandService
        );
    }

    @Test
    void supports_MESSAGE_READ를반환한다() {
        assertEquals(GmailHistoryEventType.MESSAGE_READ, handler.supports());
    }

    @Test
    void handle_이미메시지가있으면Gmail조회없이읽음처리를위임한다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount found = createMailAccount(mailAccountId);
        MailAccount refreshed = createMailAccount(mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId, GmailHistoryEventType.MESSAGE_READ);

        when(mailAccountQueryService.findSyncableMailAccountById(mailAccountId)).thenReturn(found);
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(found)).thenReturn(refreshed);
        when(gmailHistoryStateQueryService.existsMessage(mailAccountId, "thread-1", "message-1")).thenReturn(true);

        handler.handle(event);

        ArgumentCaptor<InitialMailSyncThreadSaveCommand> commandCaptor =
                ArgumentCaptor.forClass(InitialMailSyncThreadSaveCommand.class);
        verify(gmailHistoryStateApplyCommandService).applyMessageReadState(eq(refreshed), eq(event), eq(true), commandCaptor.capture());
        assertNull(commandCaptor.getValue());
        verify(gmailMessageApiService, never()).getThreads("access-token", List.of("thread-1"));
    }

    @Test
    void handle_메시지가없으면GmailThread스냅샷을조회해전달한다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId, GmailHistoryEventType.MESSAGE_READ);

        when(mailAccountQueryService.findSyncableMailAccountById(mailAccountId)).thenReturn(mailAccount);
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).thenReturn(mailAccount);
        when(gmailHistoryStateQueryService.existsMessage(mailAccountId, "thread-1", "message-1")).thenReturn(false);
        when(gmailMessageApiService.getThreads("access-token", List.of("thread-1")))
                .thenReturn(List.of(new InitialMailSyncThreadResult("thread-1", "history-1", List.of())));

        handler.handle(event);

        ArgumentCaptor<InitialMailSyncThreadSaveCommand> commandCaptor =
                ArgumentCaptor.forClass(InitialMailSyncThreadSaveCommand.class);
        verify(gmailHistoryStateApplyCommandService).applyMessageReadState(eq(mailAccount), eq(event), eq(true), commandCaptor.capture());
        assertEquals("thread-1", commandCaptor.getValue().gmailThreadId());
    }

    @Test
    void handle_메시지가없고GmailThread조회가비어있으면예외를던진다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId, GmailHistoryEventType.MESSAGE_READ);

        when(mailAccountQueryService.findSyncableMailAccountById(mailAccountId)).thenReturn(mailAccount);
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).thenReturn(mailAccount);
        when(gmailHistoryStateQueryService.existsMessage(mailAccountId, "thread-1", "message-1")).thenReturn(false);
        when(gmailMessageApiService.getThreads("access-token", List.of("thread-1"))).thenReturn(List.of());

        MailPushException exception = assertThrows(MailPushException.class, () -> handler.handle(event));

        assertEquals(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID, exception.getErrorCode());
    }

    private MailAccount createMailAccount(UUID mailAccountId) {
        return MailAccount.builder()
                .id(mailAccountId)
                .accessToken("access-token")
                .build();
    }

    private GmailHistoryEvent createEvent(UUID mailAccountId, GmailHistoryEventType eventType) {
        return new GmailHistoryEvent(eventType, mailAccountId, "message-1", "thread-1", "history-1");
    }
}
