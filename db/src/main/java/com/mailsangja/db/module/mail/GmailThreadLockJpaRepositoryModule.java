package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.GmailThreadLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GmailThreadLockJpaRepositoryModule extends JpaRepository<GmailThreadLock, UUID> {

    Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT gtl
            FROM GmailThreadLock gtl
            WHERE gtl.mailAccount.id = :mailAccountId
              AND gtl.gmailThreadId = :gmailThreadId
              AND gtl.deletedAt IS NULL
            """)
    Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );
}
