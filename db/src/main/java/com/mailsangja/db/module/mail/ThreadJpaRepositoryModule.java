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
    @Query("SELECT t FROM Thread t WHERE t.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL) AND t.direction = 'INBOUND' AND t.deletedAt IS NULL AND (:markerId IS NULL OR t.lastMessageAt < (SELECT m.lastMessageAt FROM Thread m WHERE m.id = :markerId AND m.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL) AND m.direction = 'INBOUND')) ORDER BY t.lastMessageAt DESC")
    Slice<Thread> findInboxByUserIdAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("SELECT t FROM Thread t WHERE t.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL) AND t.direction = 'OUTBOUND' AND t.deletedAt IS NULL AND (:markerId IS NULL OR t.lastMessageAt < (SELECT m.lastMessageAt FROM Thread m WHERE m.id = :markerId AND m.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL) AND m.direction = 'OUTBOUND')) ORDER BY t.lastMessageAt DESC")
    Slice<Thread> findSentByUserIdAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    Optional<Thread> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
            UUID mailAccountId, String gmailThreadId, com.mailsangja.db.entity.mail.Direction direction
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("SELECT t FROM Thread t WHERE t.mailAccount.id = :mailAccountId AND t.gmailThreadId = :gmailThreadId AND t.deletedAt IS NULL")
    List<Thread> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @Query("SELECT COUNT(t) FROM Thread t WHERE t.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL) AND t.direction = 'INBOUND' AND t.read = false AND t.deletedAt IS NULL")
    long countUnreadInboxByUserId(@Param("userId") UUID userId);
}
