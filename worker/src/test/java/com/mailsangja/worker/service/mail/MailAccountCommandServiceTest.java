package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.mail.watch.RenewGoogleWatchCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailAccountCommandServiceTest {

    @Mock
    private MailAccountRepositoryPort mailAccountRepositoryPort;

    @Test
    void renewGoogleWatch_액세스토큰이일치하면토큰과워치정보를함께갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount("old-access-token", "old-refresh-token");
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccount.getId()))
                .thenReturn(Optional.of(mailAccount));
        when(mailAccountRepositoryPort.renewGoogleWatchIfAccessTokenMatches(
                eq(mailAccount.getId()),
                eq("old-access-token"),
                eq("new-access-token"),
                any(LocalDateTime.class),
                eq("new-refresh-token"),
                eq("history-123"),
                any(LocalDateTime.class)
        )).thenReturn(1);

        MailAccountCommandService service = createService();
        LocalDateTime newWatchExpiresAt = LocalDateTime.now().plusDays(7);

        // when
        service.renewGoogleWatch(
                RenewGoogleWatchCommand.of(
                        mailAccount.getId(),
                        new GoogleOAuthTokenResult("new-access-token", "new-refresh-token", 3600L, null, "Bearer"),
                        new GoogleMailWatchResult("history-123", newWatchExpiresAt)
                )
        );

        // then
        ArgumentCaptor<LocalDateTime> tokenExpiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mailAccountRepositoryPort).renewGoogleWatchIfAccessTokenMatches(
                eq(mailAccount.getId()),
                eq("old-access-token"),
                eq("new-access-token"),
                tokenExpiresAtCaptor.capture(),
                eq("new-refresh-token"),
                eq("history-123"),
                eq(newWatchExpiresAt)
        );
    }

    @Test
    void renewGoogleWatch_경쟁상황으로조건부업데이트에실패하면기존값을유지한다() {
        // given
        MailAccount mailAccount = createMailAccount("latest-access-token", "latest-refresh-token");
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccount.getId()))
                .thenReturn(Optional.of(mailAccount));
        when(mailAccountRepositoryPort.renewGoogleWatchIfAccessTokenMatches(
                eq(mailAccount.getId()),
                eq("latest-access-token"),
                eq("stale-new-access-token"),
                any(LocalDateTime.class),
                eq("stale-new-refresh-token"),
                eq("stale-history"),
                any(LocalDateTime.class)
        )).thenReturn(0);

        MailAccountCommandService service = createService();

        // when
        service.renewGoogleWatch(
                RenewGoogleWatchCommand.of(
                        mailAccount.getId(),
                        new GoogleOAuthTokenResult("stale-new-access-token", "stale-new-refresh-token", 3600L, null, "Bearer"),
                        new GoogleMailWatchResult("stale-history", LocalDateTime.now().plusDays(1))
                )
        );

        // then
        verify(mailAccountRepositoryPort).renewGoogleWatchIfAccessTokenMatches(
                eq(mailAccount.getId()),
                eq("latest-access-token"),
                eq("stale-new-access-token"),
                any(LocalDateTime.class),
                eq("stale-new-refresh-token"),
                eq("stale-history"),
                any(LocalDateTime.class)
        );
        verify(mailAccountRepositoryPort, never()).save(any());
        assertEquals("latest-access-token", mailAccount.getAccessToken());
        assertEquals("latest-refresh-token", mailAccount.getRefreshToken());
        assertEquals("sync-history-id", mailAccount.getSyncHistoryId());
    }

    @Test
    void updateSyncHistoryId_히스토리Id를갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount("access-token", "refresh-token");
        MailAccountCommandService service = createService();

        // when
        service.updateSyncHistoryId(mailAccount, "new-history-id");

        // then
        assertEquals("new-history-id", mailAccount.getSyncHistoryId());
    }

    @Test
    void updateSyncHistoryId_메일계정이null이면예외를던진다() {
        // given
        MailAccountCommandService service = createService();

        // when
        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.updateSyncHistoryId(null, "new-history-id")
        );

        // then
        assertEquals(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION, exception.getErrorCode());
    }

    @Test
    void updateSyncHistoryId_히스토리Id가blank이면예외를던진다() {
        // given
        MailAccount mailAccount = createMailAccount("access-token", "refresh-token");
        MailAccountCommandService service = createService();

        // when
        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.updateSyncHistoryId(mailAccount, " ")
        );

        // then
        assertEquals(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION, exception.getErrorCode());
        assertEquals("sync-history-id", mailAccount.getSyncHistoryId());
    }

    @Test
    void renewGoogleWatch_명령이null이면예외를던지고갱신하지않는다() {
        // given
        MailAccountCommandService service = createService();

        // when
        MailPushException exception = assertThrows(MailPushException.class, () -> service.renewGoogleWatch(null));

        // then
        assertEquals(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST, exception.getErrorCode());
        verify(mailAccountRepositoryPort, never()).renewGoogleWatchIfAccessTokenMatches(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void refreshGoogleAccessToken_새리프레시토큰이없으면기존리프레시토큰을유지한다() {
        // given
        MailAccount mailAccount = createMailAccount("old-access-token", "old-refresh-token");
        MailAccount refreshedMailAccount = createMailAccount("new-access-token", "old-refresh-token");
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccount.getId()))
                .thenReturn(Optional.of(mailAccount))
                .thenReturn(Optional.of(refreshedMailAccount));
        MailAccountCommandService service = createService();

        // when
        MailAccount result = service.refreshGoogleAccessToken(
                mailAccount.getId(),
                new GoogleOAuthTokenResult("new-access-token", " ", 3600L, null, "Bearer")
        );

        // then
        assertEquals(refreshedMailAccount, result);
        verify(mailAccountRepositoryPort).updateGoogleTokenIfAccessTokenMatches(
                eq(mailAccount.getId()),
                eq("old-access-token"),
                eq("new-access-token"),
                any(LocalDateTime.class),
                eq("old-refresh-token")
        );
    }

    @Test
    void refreshGoogleAccessToken_입력이잘못되면예외를던지고갱신하지않는다() {
        // given
        MailAccountCommandService service = createService();

        // when
        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.refreshGoogleAccessToken(UUID.randomUUID(), new GoogleOAuthTokenResult(" ", null, 3600L, null, "Bearer"))
        );

        // then
        assertEquals(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED, exception.getErrorCode());
        verify(mailAccountRepositoryPort, never()).updateGoogleTokenIfAccessTokenMatches(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private MailAccountCommandService createService() {
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(mailAccountRepositoryPort);
        return new MailAccountCommandService(mailAccountRepositoryPort, mailAccountQueryService);
    }

    private MailAccount createMailAccount(String accessToken, String refreshToken) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("alias")
                .icon("icon")
                .color("#4285F4")
                .accessToken(accessToken)
                .accessTokenExpiresAt(LocalDateTime.now().plusMinutes(30))
                .refreshToken(refreshToken)
                .syncHistoryId("sync-history-id")
                .watchExpiresAt(LocalDateTime.now().plusDays(3))
                .active(true)
                .build();
    }
}
