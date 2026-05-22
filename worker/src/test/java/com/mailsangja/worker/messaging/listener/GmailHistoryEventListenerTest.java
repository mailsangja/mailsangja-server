package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.handler.mail.GmailHistoryEventHandler;
import com.mailsangja.worker.handler.mail.MessageAddedHistoryEventHandler;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailHistoryEventListenerTest {

    @Mock
    private MessageAddedHistoryEventHandler messageAddedHandler;

    @Mock
    private GmailHistoryEventHandler stateChangeHandler;

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;

    private GmailHistoryEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new GmailHistoryEventListener(
                messageAddedHandler,
                List.of(stateChangeHandler),
                mailAccountQueryService,
                googleAccessTokenEnsureService
        );
    }

    @Test
    void handleMessageAdded_토큰갱신후전용핸들러에위임한다() {
        UUID mailAccountId = UUID.randomUUID();
        GmailHistoryEvent event = createEvent(GmailHistoryEventType.MESSAGE_ADDED, mailAccountId);
        MailAccount found = MailAccount.builder().id(mailAccountId).accessToken("old-token").build();
        MailAccount refreshed = MailAccount.builder().id(mailAccountId).accessToken("new-token").build();

        when(mailAccountQueryService.findSyncableMailAccountById(mailAccountId)).thenReturn(found);
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(found)).thenReturn(refreshed);

        listener.handleMessageAdded(event);

        verify(messageAddedHandler).handle(refreshed, event);
    }

    @Test
    void handleStateChange_지원하는상태핸들러에위임한다() {
        UUID mailAccountId = UUID.randomUUID();
        GmailHistoryEvent event = createEvent(GmailHistoryEventType.MESSAGE_TRASHED, mailAccountId);

        when(stateChangeHandler.supports()).thenReturn(GmailHistoryEventType.MESSAGE_TRASHED);

        listener.handleStateChange(event);

        verify(stateChangeHandler).handle(event);
        verify(mailAccountQueryService, never()).findSyncableMailAccountById(mailAccountId);
    }

    @Test
    void handleStateChange_지원핸들러가없으면GmailHistory예외를던진다() {
        GmailHistoryEvent event = createEvent(GmailHistoryEventType.MESSAGE_RESTORED, UUID.randomUUID());

        when(stateChangeHandler.supports()).thenReturn(GmailHistoryEventType.MESSAGE_TRASHED);

        MailPushException exception = assertThrows(MailPushException.class, () -> listener.handleStateChange(event));

        assertEquals(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID, exception.getErrorCode());
        verify(stateChangeHandler, never()).handle(event);
    }

    private GmailHistoryEvent createEvent(GmailHistoryEventType eventType, UUID mailAccountId) {
        return new GmailHistoryEvent(
                eventType,
                mailAccountId,
                "message-1",
                "thread-1",
                "history-1"
        );
    }
}
