package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.service.google.GmailMessageApiService;
import com.mailsangja.worker.service.mail.GmailHistoryStateApplyCommandService;
import com.mailsangja.worker.service.mail.GmailHistoryStateQueryService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageUnreadHistoryEventHandlerTest {

    @Mock private MailAccountQueryService mailAccountQueryService;
    @Mock private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    @Mock private GmailHistoryStateQueryService gmailHistoryStateQueryService;
    @Mock private GmailMessageApiService gmailMessageApiService;
    @Mock private GmailHistoryStateApplyCommandService gmailHistoryStateApplyCommandService;

    private MessageUnreadHistoryEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MessageUnreadHistoryEventHandler(
                mailAccountQueryService,
                googleAccessTokenEnsureService,
                gmailHistoryStateQueryService,
                gmailMessageApiService,
                gmailHistoryStateApplyCommandService
        );
    }

    @Test
    void supports_MESSAGE_UNREAD를반환한다() {
        assertEquals(GmailHistoryEventType.MESSAGE_UNREAD, handler.supports());
    }

    @Test
    void handle_이미메시지가있으면안읽음처리를위임한다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .emailAddress("alice@example.com")
                .accessToken("access-token")
                .build();
        GmailHistoryEvent event = new GmailHistoryEvent(
                GmailHistoryEventType.MESSAGE_UNREAD,
                mailAccountId,
                "message-1",
                "thread-1",
                "history-1"
        );

        when(mailAccountQueryService.findSyncableMailAccountById(mailAccountId)).thenReturn(mailAccount);
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).thenReturn(mailAccount);
        when(gmailHistoryStateQueryService.existsMessage(mailAccountId, "thread-1", "message-1")).thenReturn(true);

        handler.handle(event);

        verify(gmailHistoryStateApplyCommandService).applyMessageReadState(mailAccount, event, false, null);
    }
}
