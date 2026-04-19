package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailAccountRepositoryPort {
    MailAccount save(MailAccount mailAccount);
    Optional<MailAccount> findByIdAndDeletedAtIsNull(UUID id);
    Optional<MailAccount> findByIdAndActiveAndDeletedAtIsNull(UUID id, boolean active);
    Optional<MailAccount> findByEmailAddressAndDeletedAtIsNull(String emailAddress);
    Optional<MailAccount> findByUserIdAndProviderAndDeletedAtIsNull(UUID userId, MailProvider provider);
    Optional<MailAccount> findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(UUID userId, MailProvider provider, String emailAddress);
    Optional<MailAccount> findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(UUID userId, String emailAddress, boolean active);
    Optional<MailAccount> findByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider provider, String emailAddress);
    List<MailAccount> findAllByUserIdAndDeletedAtIsNull(UUID userId);
    int updateGoogleTokenIfAccessTokenMatches(
            UUID id,
            String expectedAccessToken,
            String newAccessToken,
            LocalDateTime newAccessTokenExpiresAt,
            String newRefreshToken
    );
    int renewGoogleWatchIfAccessTokenMatches(
            UUID id,
            String expectedAccessToken,
            String newAccessToken,
            LocalDateTime newAccessTokenExpiresAt,
            String newRefreshToken,
            String newSyncHistoryId,
            LocalDateTime newWatchExpiresAt
    );
    List<MailAccount> findRenewalTargetGmailAccounts(MailProvider provider, LocalDateTime watchExpiresAtThreshold, int limit);
    List<MailAccount> findAllByUserIdAndActiveAndDeletedAtIsNull(UUID userId, boolean active);
}
