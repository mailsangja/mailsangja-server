package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.mail.watch.RenewGoogleWatchCommand;
import com.mailsangja.worker.dto.mail.watch.WatchRenewalMessage;
import com.mailsangja.worker.service.google.GmailWatchApiService;
import com.mailsangja.worker.service.google.GoogleOAuthApiService;
import com.mailsangja.worker.service.mail.MailAccountCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import com.mailsangja.worker.service.notification.FcmPushCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailWatchRenewalListenerTest {

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private MailAccountCommandService mailAccountCommandService;

    @Mock
    private GoogleOAuthApiService googleOAuthApiService;

    @Mock
    private GmailWatchApiService gmailWatchApiService;

    @Mock
    private FcmPushCommandService fcmPushCommandService;

    @Test
    void handle_watch갱신성공후재연동요청푸시를발송한다() {
        UUID userId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(userId, mailAccountId);
        WatchRenewalMessage message = WatchRenewalMessage.from(mailAccount);
        GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult(
                "new-access-token",
                null,
                3600L,
                null,
                "Bearer"
        );
        GoogleMailWatchResult watchResult = new GoogleMailWatchResult(
                "history-2",
                LocalDateTime.of(2026, 6, 12, 9, 0)
        );
        GmailWatchRenewalListener listener = new GmailWatchRenewalListener(
                mailAccountQueryService,
                mailAccountCommandService,
                googleOAuthApiService,
                gmailWatchApiService,
                fcmPushCommandService
        );
        when(mailAccountQueryService.findSyncableMailAccountById(mailAccountId)).thenReturn(mailAccount);
        when(googleOAuthApiService.refreshAccessToken("refresh-token")).thenReturn(tokenResult);
        when(gmailWatchApiService.watch("new-access-token")).thenReturn(watchResult);

        listener.handle(message);

        ArgumentCaptor<RenewGoogleWatchCommand> commandCaptor = ArgumentCaptor.forClass(RenewGoogleWatchCommand.class);
        InOrder inOrder = inOrder(mailAccountCommandService, fcmPushCommandService);
        inOrder.verify(mailAccountCommandService).renewGoogleWatch(commandCaptor.capture());
        inOrder.verify(fcmPushCommandService).sendGmailReauthorizationRequestPush(mailAccount);
        assertEquals(mailAccountId, commandCaptor.getValue().mailAccountId());
        assertEquals(tokenResult, commandCaptor.getValue().tokenResult());
        assertEquals(watchResult, commandCaptor.getValue().watchResult());
        verify(googleOAuthApiService).refreshAccessToken("refresh-token");
        verify(gmailWatchApiService).watch("new-access-token");
    }

    @Test
    void handle_재연동요청푸시가실패해도watch갱신성공은유지한다() {
        UUID userId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(userId, mailAccountId);
        WatchRenewalMessage message = WatchRenewalMessage.from(mailAccount);
        GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult(
                "new-access-token",
                null,
                3600L,
                null,
                "Bearer"
        );
        GoogleMailWatchResult watchResult = new GoogleMailWatchResult(
                "history-2",
                LocalDateTime.of(2026, 6, 12, 9, 0)
        );
        GmailWatchRenewalListener listener = new GmailWatchRenewalListener(
                mailAccountQueryService,
                mailAccountCommandService,
                googleOAuthApiService,
                gmailWatchApiService,
                fcmPushCommandService
        );
        when(mailAccountQueryService.findSyncableMailAccountById(mailAccountId)).thenReturn(mailAccount);
        when(googleOAuthApiService.refreshAccessToken("refresh-token")).thenReturn(tokenResult);
        when(gmailWatchApiService.watch("new-access-token")).thenReturn(watchResult);
        doThrow(new RuntimeException("FCM failed"))
                .when(fcmPushCommandService)
                .sendGmailReauthorizationRequestPush(mailAccount);

        listener.handle(message);

        ArgumentCaptor<RenewGoogleWatchCommand> commandCaptor = ArgumentCaptor.forClass(RenewGoogleWatchCommand.class);
        verify(mailAccountCommandService).renewGoogleWatch(commandCaptor.capture());
        verify(fcmPushCommandService).sendGmailReauthorizationRequestPush(mailAccount);
        assertEquals(mailAccountId, commandCaptor.getValue().mailAccountId());
        assertEquals(tokenResult, commandCaptor.getValue().tokenResult());
        assertEquals(watchResult, commandCaptor.getValue().watchResult());
    }

    private MailAccount createMailAccount(UUID userId, UUID mailAccountId) {
        return MailAccount.builder()
                .id(mailAccountId)
                .user(User.builder().id(userId).build())
                .provider(MailProvider.GMAIL)
                .emailAddress("gmail@example.com")
                .alias("업무용")
                .icon("good")
                .color("#123456")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 6, 5, 9, 0))
                .refreshToken("refresh-token")
                .active(true)
                .syncHistoryId("history-1")
                .watchExpiresAt(LocalDateTime.of(2026, 6, 6, 9, 0))
                .build();
    }
}
