package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailAccountJpaRepositoryModule extends JpaRepository<MailAccount, UUID> {

    Optional<MailAccount> findByEmailAddressAndDeletedAtIsNull(String emailAddress);

    Optional<MailAccount> findByUserIdAndProviderAndDeletedAtIsNull(UUID userId, MailProvider provider);

    Optional<MailAccount> findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(UUID userId, MailProvider provider, String emailAddress);

    @EntityGraph(attributePaths = {"user"})
    Optional<MailAccount> findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(UUID userId, String emailAddress, boolean active);

    @EntityGraph(attributePaths = {"user"})
    Optional<MailAccount> findByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider provider, String emailAddress);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT ma FROM MailAccount ma WHERE ma.id = :id AND ma.deletedAt IS NULL")
    Optional<MailAccount> findByIdAndDeletedAtIsNull(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"user"})
    Optional<MailAccount> findByIdAndActiveAndDeletedAtIsNull(UUID id, boolean active);

    @EntityGraph(attributePaths = {"user"})
    List<MailAccount> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    @EntityGraph(attributePaths = {"user"})
    List<MailAccount> findAllByUserIdAndActiveAndDeletedAtIsNull(UUID userId, boolean active);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT ma
            FROM MailAccount ma
            WHERE ma.provider = :provider
              AND ma.active = true
              AND ma.deletedAt IS NULL
              AND ma.watchExpiresAt IS NOT NULL
              AND ma.watchExpiresAt <= :watchExpiresAtThreshold
            ORDER BY ma.watchExpiresAt ASC
            """)
    List<MailAccount> findRenewalTargetGmailAccounts(
            @Param("provider") MailProvider provider,
            @Param("watchExpiresAtThreshold") LocalDateTime watchExpiresAtThreshold
    );
}
