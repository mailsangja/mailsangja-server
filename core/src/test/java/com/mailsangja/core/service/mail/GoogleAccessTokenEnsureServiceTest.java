package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleOAuthProperties;
import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Clock;
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
        verify(mailAccountRepositoryPort, never()).findByIdAndActiveAndDeletedAtIsNull(any(), eq(true));
        verify(mailAccountRepositoryPort, never()).updateGoogleTokenIfAccessTokenMatches(any(), any(), any(), any(), any());
    }

    @Test
    void ensureValidGoogleAccessToken_만료가임박하면토큰을재발급한다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                "old-token",
                "refresh-token"
        );
        when(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(mailAccount.getId(), true))
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
    void ensureValidGoogleAccessToken_토큰재발급에실패하면리프레시토큰을삭제한다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                "old-token",
                "refresh-token"
        );
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        googleOAuthQueryService.failRefresh = true;
        GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.ensureValidGoogleAccessToken(mailAccount)
        );

        assertEquals(MailAccountErrorCode.GOOGLE_TOKEN_REFRESH_FAILED, exception.getErrorCode());
        verify(mailAccountRepositoryPort).clearRefreshToken(mailAccount.getId());
    }

    @Test
    void ensureValidGoogleAccessToken_토큰재발급실패가아닌예외면리프레시토큰을삭제하지않는다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                "old-token",
                "refresh-token"
        );
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        googleOAuthQueryService.failRefresh = true;
        googleOAuthQueryService.refreshFailureErrorCode = MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND;
        GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.ensureValidGoogleAccessToken(mailAccount)
        );

        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND, exception.getErrorCode());
        verify(mailAccountRepositoryPort, never()).clearRefreshToken(mailAccount.getId());
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

        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.ensureValidGoogleAccessToken(mailAccount)
        );

        assertEquals(MailAccountErrorCode.GOOGLE_REFRESH_TOKEN_MISSING, exception.getErrorCode());
        assertEquals(0, googleOAuthQueryService.refreshCallCount);
        verify(mailAccountRepositoryPort, never()).updateGoogleTokenIfAccessTokenMatches(any(), any(), any(), any(), any());
    }

    @Test
    void ensureValidGoogleAccessToken_비활성계정이면비활성예외를던진다() {
        MailAccount mailAccount = createMailAccount(
                LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                "old-token",
                "refresh-token"
        );
        mailAccount.deactivate();
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.ensureValidGoogleAccessToken(mailAccount)
        );

        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_INACTIVE, exception.getErrorCode());
        assertEquals(0, googleOAuthQueryService.refreshCallCount);
        verify(mailAccountRepositoryPort, never()).updateGoogleTokenIfAccessTokenMatches(any(), any(), any(), any(), any());
    }

    private GoogleAccessTokenEnsureService createService(FakeGoogleOAuthQueryService googleOAuthQueryService) {
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(mailAccountRepositoryPort);
        MailAccountCommandService mailAccountCommandService = new MailAccountCommandService(
                mailAccountRepositoryPort,
                Clock.systemUTC()
        );
        return new GoogleAccessTokenEnsureService(
                mailAccountQueryService,
                mailAccountCommandService,
                googleOAuthQueryService
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
        private boolean failRefresh;
        private MailAccountErrorCode refreshFailureErrorCode = MailAccountErrorCode.GOOGLE_TOKEN_REFRESH_FAILED;

        private FakeGoogleOAuthQueryService() {
            super(new GoogleOAuthProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleOAuthTokenResult refreshAccessToken(String refreshToken) {
            refreshCallCount++;
            if (failRefresh) {
                throw new MailAccountException(refreshFailureErrorCode);
            }
            return new GoogleOAuthTokenResult("refreshed-token", "new-refresh-token", 3600L, null, "Bearer");
        }
    }
}
