package com.mailsangja.core.service.mail;

import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.core.config.properties.GoogleOAuthProperties;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleAccessTokenEnsureServiceTest {

    @Test
    void ensureValidGoogleAccessToken_만료가충분히남아있으면기존토큰을사용한다() {
        MailAccount mailAccount = createMailAccount(LocalDateTime.now().plusMinutes(30), "access-token", "refresh-token");
        InMemoryMailAccountRepository repository = new InMemoryMailAccountRepository(mailAccount);
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        GoogleAccessTokenEnsureService service = createService(repository, googleOAuthQueryService);

        MailAccount ensuredMailAccount = service.ensureValidGoogleAccessToken(mailAccount);

        assertEquals("access-token", ensuredMailAccount.getAccessToken());
        assertEquals(0, googleOAuthQueryService.refreshCallCount);
    }

    @Test
    void ensureValidGoogleAccessToken_만료가임박하면토큰을재발급한다() {
        MailAccount mailAccount = createMailAccount(LocalDateTime.now().plusMinutes(5), "old-token", "refresh-token");
        InMemoryMailAccountRepository repository = new InMemoryMailAccountRepository(mailAccount);
        FakeGoogleOAuthQueryService googleOAuthQueryService = new FakeGoogleOAuthQueryService();
        GoogleAccessTokenEnsureService service = createService(repository, googleOAuthQueryService);

        MailAccount ensuredMailAccount = service.ensureValidGoogleAccessToken(mailAccount);

        assertEquals("refreshed-token", ensuredMailAccount.getAccessToken());
        assertEquals("new-refresh-token", ensuredMailAccount.getRefreshToken());
        assertEquals(1, googleOAuthQueryService.refreshCallCount);
    }

    private GoogleAccessTokenEnsureService createService(
            InMemoryMailAccountRepository repository,
            FakeGoogleOAuthQueryService googleOAuthQueryService
    ) {
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(repository);
        MailAccountCommandService mailAccountCommandService = new MailAccountCommandService(
                repository,
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

    private static final class InMemoryMailAccountRepository implements MailAccountRepositoryPort {
        private final MailAccount mailAccount;

        private InMemoryMailAccountRepository(MailAccount mailAccount) {
            this.mailAccount = mailAccount;
        }

        @Override
        public MailAccount save(MailAccount mailAccount) {
            return this.mailAccount;
        }

        @Override
        public Optional<MailAccount> findByIdAndDeletedAtIsNull(UUID id) {
            return Optional.of(mailAccount).filter(account -> id.equals(account.getId()));
        }

        @Override
        public Optional<MailAccount> findByIdAndActiveAndDeletedAtIsNull(UUID id, boolean active) {
            return Optional.of(mailAccount)
                    .filter(account -> id.equals(account.getId()))
                    .filter(account -> account.isActive() == active);
        }

        @Override
        public Optional<MailAccount> findByEmailAddressAndDeletedAtIsNull(String emailAddress) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByUserIdAndProviderAndDeletedAtIsNull(UUID userId, MailProvider provider) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(UUID userId, MailProvider provider, String emailAddress) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(UUID userId, String emailAddress, boolean active) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider provider, String emailAddress) {
            return Optional.empty();
        }

        @Override
        public List<MailAccount> findAllByUserIdAndDeletedAtIsNull(UUID userId) {
            return List.of();
        }

        @Override
        public List<MailAccount> findRenewalTargetGmailAccounts(MailProvider provider, LocalDateTime watchExpiresAtThreshold, int limit) {
            return List.of();
        }

        @Override
        public List<MailAccount> findAllByUserIdAndActiveAndDeletedAtIsNull(UUID userId, boolean active) {
            return List.of();
        }
    }
}
