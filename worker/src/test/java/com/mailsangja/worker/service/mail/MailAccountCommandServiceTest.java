package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
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

        service.renewGoogleWatch(
                RenewGoogleWatchCommand.of(
                        mailAccount.getId(),
                        new GoogleOAuthTokenResult("new-access-token", "new-refresh-token", 3600L, null, "Bearer"),
                        new GoogleMailWatchResult("history-123", newWatchExpiresAt)
                )
        );

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

        service.renewGoogleWatch(
                RenewGoogleWatchCommand.of(
                        mailAccount.getId(),
                        new GoogleOAuthTokenResult("stale-new-access-token", "stale-new-refresh-token", 3600L, null, "Bearer"),
                        new GoogleMailWatchResult("stale-history", LocalDateTime.now().plusDays(1))
                )
        );

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
