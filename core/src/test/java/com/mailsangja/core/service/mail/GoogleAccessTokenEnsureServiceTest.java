package com.mailsangja.core.service.mail;

import com.mailsangja.core.config.properties.GoogleOAuthProperties;
import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleAccessTokenEnsureService 테스트")
class GoogleAccessTokenEnsureServiceTest {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Mock
    private MailAccountRepositoryPort mailAccountRepositoryPort;

    @Nested
    @DisplayName("ensureValidGoogleAccessToken")
    class EnsureValidGoogleAccessToken {

        @Test
        @DisplayName("만료가 충분히 남아 있으면 기존 토큰을 사용한다")
        void ensureValidGoogleAccessToken_만료가충분히남아있으면기존토큰을사용한다() {
            // given
            MailAccount mailAccount = createMailAccount(
                    LocalDateTime.now(KST_ZONE_ID).plusMinutes(30),
                    "access-token",
                    "refresh-token"
            );
            FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
            GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

            // when
            MailAccount ensuredMailAccount = service.ensureValidGoogleAccessToken(mailAccount);

            // then
            assertSame(mailAccount, ensuredMailAccount);
            assertEquals(0, googleOAuthQueryService.refreshCallCount);
            then(mailAccountRepositoryPort).should(never()).findByIdAndActiveAndDeletedAtIsNull(any(), eq(true));
            then(mailAccountRepositoryPort).should(never()).updateGoogleTokenIfAccessTokenMatches(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("만료가 임박하면 토큰을 재발급한다")
        void ensureValidGoogleAccessToken_만료가임박하면토큰을재발급한다() {
            // given
            MailAccount mailAccount = createMailAccount(
                    LocalDateTime.now(KST_ZONE_ID).plusMinutes(5),
                    "old-token",
                    "refresh-token"
            );
            given(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(mailAccount.getId(), true))
                    .willReturn(Optional.of(mailAccount));
            given(mailAccountRepositoryPort.updateGoogleTokenIfAccessTokenMatches(
                    eq(mailAccount.getId()),
                    eq("old-token"),
                    eq("refreshed-token"),
                    any(LocalDateTime.class),
                    eq("new-refresh-token")
            )).willAnswer(invocation -> {
                mailAccount.updateAccessToken(invocation.getArgument(2));
                mailAccount.updateAccessTokenExpiresAt(invocation.getArgument(3));
                mailAccount.updateRefreshToken(invocation.getArgument(4));
                return 1;
            });

            FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
            GoogleAccessTokenEnsureService service = createService(googleOAuthQueryService);

            // when
            MailAccount ensuredMailAccount = service.ensureValidGoogleAccessToken(mailAccount);

            // then
            ArgumentCaptor<LocalDateTime> expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            then(mailAccountRepositoryPort).should().updateGoogleTokenIfAccessTokenMatches(
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
    }

    private GoogleAccessTokenEnsureService createService(FakeGoogleOAuthQueryService googleOAuthQueryService) {
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(mailAccountRepositoryPort);
        MailAccountCommandService mailAccountCommandService = new MailAccountCommandService(
                mailAccountRepositoryPort,
                mailAccountQueryService
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
