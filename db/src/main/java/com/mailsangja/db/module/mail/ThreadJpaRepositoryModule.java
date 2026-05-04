package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.Thread;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
    @Query("""
            SELECT t FROM Thread t
            WHERE t.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND t.direction = 'INBOUND'
              AND t.deletedAt IS NULL
              AND (:read IS NULL OR t.read = :read)
              AND (:markerId IS NULL OR t.lastMessageAt < (
                SELECT m.lastMessageAt FROM Thread m
                WHERE m.id = :markerId
                  AND m.mailAccount.id IN (
                    SELECT ma.id FROM MailAccount ma
                    WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
                  )
                  AND m.direction = 'INBOUND'
              ))
            ORDER BY t.lastMessageAt DESC
            """)
    Slice<Thread> findInboxByUserIdAndReadFilter(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            @Param("read") Boolean read,
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
    @Query("""
            SELECT t FROM Thread t
            WHERE t.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND t.direction = 'OUTBOUND'
              AND t.deletedAt IS NULL
              AND (:read IS NULL OR t.read = :read)
              AND (:markerId IS NULL OR t.lastMessageAt < (
                SELECT m.lastMessageAt FROM Thread m
                WHERE m.id = :markerId
                  AND m.mailAccount.id IN (
                    SELECT ma.id FROM MailAccount ma
                    WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
                  )
                  AND m.direction = 'OUTBOUND'
              ))
            ORDER BY t.lastMessageAt DESC
            """)
    Slice<Thread> findSentByUserIdAndReadFilter(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            @Param("read") Boolean read,
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

    @Query("SELECT COUNT(t) FROM Thread t WHERE t.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL) AND t.direction = 'OUTBOUND' AND t.read = false AND t.deletedAt IS NULL")
    long countUnreadSentByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COUNT(DISTINCT t) FROM Thread t
            JOIN t.threadLabels tl
            WHERE t.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND t.direction = 'INBOUND'
              AND t.read = false
              AND t.deletedAt IS NULL
              AND tl.deletedAt IS NULL
              AND tl.label.id IN :labelIds
              AND tl.label.deletedAt IS NULL
            """)
    long countUnreadInboxByUserIdAndLabelIds(
            @Param("userId") UUID userId,
            @Param("labelIds") List<UUID> labelIds
    );

    @Query("""
            SELECT COUNT(DISTINCT t) FROM Thread t
            JOIN t.threadLabels tl
            WHERE t.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND t.direction = 'OUTBOUND'
              AND t.read = false
              AND t.deletedAt IS NULL
              AND tl.deletedAt IS NULL
              AND tl.label.id IN :labelIds
              AND tl.label.deletedAt IS NULL
            """)
    long countUnreadSentByUserIdAndLabelIds(
            @Param("userId") UUID userId,
            @Param("labelIds") List<UUID> labelIds
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    Optional<Thread> findById(UUID id);

    @Query("SELECT t FROM Thread t WHERE t.mailAccount.id = :mailAccountId AND t.gmailThreadId = :gmailThreadId")
    List<Thread> findAllByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Thread t SET t.deletedAt = :deletedAt WHERE t.mailAccount.id = :mailAccountId AND t.gmailThreadId = :gmailThreadId AND t.deletedAt IS NULL")
    int bulkSoftDeleteByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Thread t SET t.deletedAt = NULL WHERE t.mailAccount.id = :mailAccountId AND t.gmailThreadId = :gmailThreadId AND t.deletedAt IS NOT NULL")
    int bulkRestoreByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Thread t SET t.deletedAt = NULL, t.messageCount = 0 WHERE t.mailAccount.id = :mailAccountId AND t.gmailThreadId = :gmailThreadId AND t.deletedAt IS NOT NULL")
    int bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("SELECT t FROM Thread t WHERE t.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL) AND t.deletedAt IS NOT NULL AND (:markerId IS NULL OR t.deletedAt < (SELECT m.deletedAt FROM Thread m WHERE m.id = :markerId)) ORDER BY t.deletedAt DESC")
    Slice<Thread> findTrashByUserId(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("""
            SELECT DISTINCT t FROM Thread t
            JOIN t.threadLabels tl
            WHERE t.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
             AND t.direction = 'INBOUND'
             AND t.deletedAt IS NULL
             AND tl.deletedAt IS NULL
             AND tl.label.id IN :labelIds
             AND tl.label.deletedAt IS NULL
             AND (:markerId IS NULL OR t.lastMessageAt < (
                SELECT m.lastMessageAt FROM Thread m WHERE m.id = :markerId
                AND m.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL)
                AND m.direction = 'INBOUND'
            ))
            ORDER BY t.lastMessageAt DESC
            """)
    Slice<Thread> findInboxByUserIdAndLabelIdsAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("labelIds") List<UUID> labelIds,
            @Param("markerId") UUID markerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("""
            SELECT DISTINCT t FROM Thread t
            JOIN t.threadLabels tl
            WHERE t.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
             AND t.direction = 'INBOUND'
             AND t.deletedAt IS NULL
             AND (:read IS NULL OR t.read = :read)
             AND tl.deletedAt IS NULL
             AND tl.label.id IN :labelIds
             AND tl.label.deletedAt IS NULL
             AND (:markerId IS NULL OR t.lastMessageAt < (
                SELECT m.lastMessageAt FROM Thread m WHERE m.id = :markerId
                AND m.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL)
                AND m.direction = 'INBOUND'
            ))
            ORDER BY t.lastMessageAt DESC
            """)
    Slice<Thread> findInboxByUserIdAndLabelIdsAndReadFilter(
            @Param("userId") UUID userId,
            @Param("labelIds") List<UUID> labelIds,
            @Param("markerId") UUID markerId,
            @Param("read") Boolean read,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("""
            SELECT DISTINCT t FROM Thread t
            JOIN t.threadLabels tl
            WHERE t.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
             AND t.direction = 'OUTBOUND'
             AND t.deletedAt IS NULL
             AND (:read IS NULL OR t.read = :read)
             AND tl.deletedAt IS NULL
             AND tl.label.id IN :labelIds
             AND tl.label.deletedAt IS NULL
             AND (:markerId IS NULL OR t.lastMessageAt < (
                SELECT m.lastMessageAt FROM Thread m WHERE m.id = :markerId
                AND m.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL)
                AND m.direction = 'OUTBOUND'
            ))
            ORDER BY t.lastMessageAt DESC
            """)
    Slice<Thread> findSentByUserIdAndLabelIdsAndReadFilter(
            @Param("userId") UUID userId,
            @Param("labelIds") List<UUID> labelIds,
            @Param("markerId") UUID markerId,
            @Param("read") Boolean read,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ThreadLabel tl WHERE tl.label.id = :labelId")
    int deleteThreadLabelsByLabelId(@Param("labelId") UUID labelId);
}
