package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.Thread;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreadJpaRepositoryModule extends JpaRepository<Thread, UUID> {

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("SELECT t FROM Thread t WHERE t.mailAccount.id IN :accountIds AND t.direction = 'INBOUND' AND t.deletedAt IS NULL AND (:markerId IS NULL OR t.lastMessageAt < (SELECT m.lastMessageAt FROM Thread m WHERE m.id = :markerId AND m.mailAccount.id IN :accountIds AND m.direction = 'INBOUND' AND m.deletedAt IS NULL)) ORDER BY t.lastMessageAt DESC")
    Slice<Thread> findInboxByMailAccountIdInAndDeletedAtIsNull(
            @Param("accountIds") List<UUID> accountIds,
            @Param("markerId") UUID markerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("SELECT t FROM Thread t WHERE t.mailAccount.id IN :accountIds AND t.direction = 'OUTBOUND' AND t.deletedAt IS NULL AND (:markerId IS NULL OR t.lastMessageAt < (SELECT m.lastMessageAt FROM Thread m WHERE m.id = :markerId AND m.mailAccount.id IN :accountIds AND m.direction = 'OUTBOUND' AND m.deletedAt IS NULL)) ORDER BY t.lastMessageAt DESC")
    Slice<Thread> findSentByMailAccountIdInAndDeletedAtIsNull(
            @Param("accountIds") List<UUID> accountIds,
            @Param("markerId") UUID markerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    Optional<Thread> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
            UUID mailAccountId, String gmailThreadId, com.mailsangja.db.entity.mail.Direction direction
    );
}
