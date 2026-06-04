package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleOAuthProperties;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.service.google.GoogleOAuthApiService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void ensureValidGoogleAccessToken_토큰재발급에실패하면재연동필요로표시한다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                "old-token",
                "refresh-token"
        );
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        googleOAuthQueryService.failRefresh = true;
        GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.ensureValidGoogleAccessToken(mailAccount)
        );

        assertEquals(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED, exception.getErrorCode());
        verify(mailAccountRepositoryPort).clearRefreshToken(mailAccount.getId());
    }

    @Test
    void ensureValidGoogleAccessToken_리프레시토큰이없으면토큰재발급을시도하지않는다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                "old-token",
                null
        );
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.ensureValidGoogleAccessToken(mailAccount)
        );

        assertEquals(MailPushErrorCode.GOOGLE_REFRESH_TOKEN_MISSING, exception.getErrorCode());
        assertEquals(0, googleOAuthQueryService.refreshCallCount);
        verify(mailAccountRepositoryPort, never()).updateGoogleTokenIfAccessTokenMatches(any(), any(), any(), any(), any());
    }

    @Test
    void ensureValidGoogleAccessToken_비활성계정이면계정상태예외를던진다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                "old-token",
                "refresh-token"
        );
        mailAccount.deactivate();
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.ensureValidGoogleAccessToken(mailAccount)
        );

        assertEquals(MailPushErrorCode.INVALID_MAIL_ACCOUNT_STATE, exception.getErrorCode());
        assertEquals(0, googleOAuthQueryService.refreshCallCount);
        verify(mailAccountRepositoryPort, never()).updateGoogleTokenIfAccessTokenMatches(any(), any(), any(), any(), any());
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
                .user(User.builder().id(UUID.randomUUID()).build())
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

    private static final class FakeGoogleOAuthQueryService extends GoogleOAuthApiService {
        private int refreshCallCount;
        private boolean failRefresh;

        private FakeGoogleOAuthQueryService() {
            super(new GoogleOAuthProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleOAuthTokenResult refreshAccessToken(String refreshToken) {
            refreshCallCount++;
            if (failRefresh) {
                throw new MailPushException(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
            }
            return new GoogleOAuthTokenResult("refreshed-token", "new-refresh-token", 3600L, null, "Bearer");
        }
    }
}
