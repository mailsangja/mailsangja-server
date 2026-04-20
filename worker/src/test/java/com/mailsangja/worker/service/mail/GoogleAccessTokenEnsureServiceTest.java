package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.config.properties.GoogleOAuthProperties;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.service.google.GoogleOAuthQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAccessTokenEnsureServiceTest {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Mock
    private MailAccountRepositoryPort mailAccountRepositoryPort;

    @Test
    void ensureValidGoogleAccessToken_만료가충분히남아있으면기존토큰을사용한다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(30),
                "access-token",
                "refresh-token"
        );
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

        MailAccount ensuredMailAccount = service.ensureValidGoogleAccessToken(mailAccount);

        assertSame(mailAccount, ensuredMailAccount);
        assertEquals(0, googleOAuthQueryService.refreshCallCount);
        verify(mailAccountRepositoryPort, never()).findByIdAndDeletedAtIsNull(any());
        verify(mailAccountRepositoryPort, never()).updateGoogleTokenIfAccessTokenMatches(any(), any(), any(), any(), any());
    }

    @Test
    void ensureValidGoogleAccessToken_만료가임박하면토큰을재발급한다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                "old-token",
                "refresh-token"
        );
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccount.getId()))
                .thenReturn(Optional.of(mailAccount));
        when(mailAccountRepositoryPort.updateGoogleTokenIfAccessTokenMatches(
                eq(mailAccount.getId()),
                eq("old-token"),
                eq("refreshed-token"),
                any(LocalDateTime.class),
                eq("new-refresh-token")
        )).thenAnswer(invocation -> {
            mailAccount.updateAccessToken(invocation.getArgument(2));
            mailAccount.updateAccessTokenExpiresAt(invocation.getArgument(3));
            mailAccount.updateRefreshToken(invocation.getArgument(4));
            return 1;
        });

        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

        MailAccount ensuredMailAccount = service.ensureValidGoogleAccessToken(mailAccount);

        ArgumentCaptor<LocalDateTime> expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mailAccountRepositoryPort).updateGoogleTokenIfAccessTokenMatches(
                eq(mailAccount.getId()),
                eq("old-token"),
                eq("refreshed-token"),
                expiresAtCaptor.capture(),
                eq("new-refresh-token")
        );
        assertEquals("refreshed-token", ensuredMailAccount.getAccessToken());
        assertEquals("new-refresh-token", ensuredMailAccount.getRefreshToken());
        assertEquals(1, googleOAuthQueryService.refreshCallCount);
        assertSame(mailAccount, ensuredMailAccount);
    }

    private GoogleAccessTokenEnsureService createService(FakeGoogleOAuthQueryService googleOAuthQueryService) {
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(mailAccountRepositoryPort);
        MailAccountCommandService mailAccountCommandService = new MailAccountCommandService(
                mailAccountRepositoryPort,
                mailAccountQueryService
        );
        return new GoogleAccessTokenEnsureService(
                mailAccountCommandService,
                googleOAuthQueryService,
                mailAccountQueryService
        );
    }

    private MailAccount createMailAccount(LocalDateTime expiresAt, String accessToken, String refreshToken) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("alias")
                .icon("icon")
                .color("#4285F4")
                .accessToken(accessToken)
                .accessTokenExpiresAt(expiresAt)
                .refreshToken(refreshToken)
                .active(true)
                .build();
    }

    private static final class FakeGoogleOAuthQueryService extends GoogleOAuthQueryService {
        private int refreshCallCount;

        private FakeGoogleOAuthQueryService() {
            super(new GoogleOAuthProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleOAuthTokenResult refreshAccessToken(String refreshToken) {
            refreshCallCount++;
            return new GoogleOAuthTokenResult("refreshed-token", "new-refresh-token", 3600L, null, "Bearer");
        }
    }
}
