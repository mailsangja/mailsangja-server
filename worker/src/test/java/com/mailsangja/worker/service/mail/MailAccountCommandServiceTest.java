package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.mail.watch.RenewGoogleWatchCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailAccountCommandServiceTest {

    @Test
    void renewGoogleWatch_액세스토큰이일치하면토큰과워치정보를함께갱신한다() {
        MailAccount mailAccount = createMailAccount("old-access-token", "old-refresh-token");
        InMemoryMailAccountRepository repository = new InMemoryMailAccountRepository(mailAccount);
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(repository);
        MailAccountCommandService service = new MailAccountCommandService(repository, mailAccountQueryService);

        service.renewGoogleWatch(
                RenewGoogleWatchCommand.of(
                        mailAccount.getId(),
                        new GoogleOAuthTokenResult("new-access-token", "new-refresh-token", 3600L, null, "Bearer"),
                        new GoogleMailWatchResult("history-123", LocalDateTime.now().plusDays(7))
                )
        );

        assertEquals("new-access-token", mailAccount.getAccessToken());
        assertEquals("new-refresh-token", mailAccount.getRefreshToken());
        assertEquals("history-123", mailAccount.getSyncHistoryId());
    }

    @Test
    void renewGoogleWatch_경쟁상황으로조건부업데이트에실패하면기존값을유지한다() {
        MailAccount mailAccount = createMailAccount("latest-access-token", "latest-refresh-token");
        InMemoryMailAccountRepository repository = new InMemoryMailAccountRepository(mailAccount);
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(repository);
        MailAccountCommandService service = new MailAccountCommandService(repository, mailAccountQueryService);
        repository.expectedAccessTokenOverride = "different-access-token";

        service.renewGoogleWatch(
                RenewGoogleWatchCommand.of(
                        mailAccount.getId(),
                        new GoogleOAuthTokenResult("stale-new-access-token", "stale-new-refresh-token", 3600L, null, "Bearer"),
                        new GoogleMailWatchResult("stale-history", LocalDateTime.now().plusDays(1))
                )
        );

        assertEquals("latest-access-token", mailAccount.getAccessToken());
        assertEquals("latest-refresh-token", mailAccount.getRefreshToken());
        assertEquals("sync-history-id", mailAccount.getSyncHistoryId());
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

    private static final class InMemoryMailAccountRepository implements MailAccountRepositoryPort {
        private final MailAccount mailAccount;
        private String expectedAccessTokenOverride;

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
        public int updateGoogleTokenIfAccessTokenMatches(
                UUID id,
                String expectedAccessToken,
                String newAccessToken,
                LocalDateTime newAccessTokenExpiresAt,
                String newRefreshToken
        ) {
            return 0;
        }

        @Override
        public int renewGoogleWatchIfAccessTokenMatches(
                UUID id,
                String expectedAccessToken,
                String newAccessToken,
                LocalDateTime newAccessTokenExpiresAt,
                String newRefreshToken,
                String newSyncHistoryId,
                LocalDateTime newWatchExpiresAt
        ) {
            String matchedAccessToken = expectedAccessTokenOverride == null
                    ? mailAccount.getAccessToken()
                    : expectedAccessTokenOverride;

            if (!id.equals(mailAccount.getId()) || !expectedAccessToken.equals(matchedAccessToken)) {
                return 0;
            }

            mailAccount.updateAccessToken(newAccessToken);
            mailAccount.updateAccessTokenExpiresAt(newAccessTokenExpiresAt);
            mailAccount.updateRefreshToken(newRefreshToken);
            mailAccount.updateSyncHistoryId(newSyncHistoryId);
            mailAccount.updateWatchExpiresAt(newWatchExpiresAt);
            return 1;
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
