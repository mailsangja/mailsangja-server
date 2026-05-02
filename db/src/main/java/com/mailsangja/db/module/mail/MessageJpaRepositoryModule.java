package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.Message;
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

public interface MessageJpaRepositoryModule extends JpaRepository<Message, UUID> {

    Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId);

    Optional<Message> findByThreadIdAndGmailMessageId(UUID threadId, String gmailMessageId);

    @Query("""
            SELECT m
            FROM Message m
            WHERE m.thread.mailAccount.id = :mailAccountId
              AND m.thread.gmailThreadId = :gmailThreadId
              AND m.gmailMessageId = :gmailMessageId
              AND m.deletedAt IS NULL
            """)
    Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId,
            @Param("gmailMessageId") String gmailMessageId
    );

    @Query("""
            SELECT m
            FROM Message m
            WHERE m.thread.mailAccount.id = :mailAccountId
              AND m.thread.gmailThreadId = :gmailThreadId
              AND m.gmailMessageId = :gmailMessageId
            """)
    Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId,
            @Param("gmailMessageId") String gmailMessageId
    );

    @Query("""
            SELECT COUNT(m) > 0
            FROM Message m
            WHERE m.thread.mailAccount.id = :mailAccountId
              AND m.thread.gmailThreadId = :gmailThreadId
              AND m.deletedAt IS NULL
              AND m.gmailMessageId <> :gmailMessageId
            """)
    boolean existsByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndGmailMessageIdNot(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId,
            @Param("gmailMessageId") String gmailMessageId
    );

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.id = :threadId AND m.deletedAt IS NULL ORDER BY m.sentAt ASC")
    List<Message> findAllByThreadIdAndDeletedAtIsNull(@Param("threadId") UUID threadId);

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.id IN :threadIds AND m.deletedAt IS NULL")
    List<Message> findAllByThreadIdInAndDeletedAtIsNull(@Param("threadIds") List<UUID> threadIds);

    // 스레드 상세 조회: INBOUND/OUTBOUND 두 Thread 행의 메시지를 모두 반환 (전체 대화)
    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.mailAccount.id = :mailAccountId AND m.thread.gmailThreadId = :gmailThreadId AND m.deletedAt IS NULL ORDER BY m.sentAt ASC")
    List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount", "thread.mailAccount.user"})
    Optional<Message> findById(UUID id);

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.mailAccount.id = :mailAccountId AND m.thread.gmailThreadId = :gmailThreadId ORDER BY m.sentAt ASC")
    List<Message> findAllByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.id = :threadId ORDER BY m.sentAt ASC")
    List<Message> findAllByThreadIdIncludingDeleted(@Param("threadId") UUID threadId);

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount"})
    @Query("SELECT m FROM Message m WHERE m.thread.mailAccount.id IN (SELECT ma.id FROM MailAccount ma WHERE ma.user.id = :userId AND ma.deletedAt IS NULL) AND m.deletedAt IS NOT NULL AND (:markerId IS NULL OR m.deletedAt < (SELECT mm.deletedAt FROM Message mm WHERE mm.id = :markerId)) ORDER BY m.deletedAt DESC")
    Slice<Message> findDeletedByUserId(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.mailAccount.id = :mailAccountId AND m.thread.gmailThreadId = :gmailThreadId AND m.deletedAt IS NOT NULL ORDER BY m.sentAt ASC")
    List<Message> findAllDeletedByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @Query("""
            SELECT COUNT(m) > 0
            FROM Message m
            WHERE m.thread.mailAccount.id = :mailAccountId
              AND m.thread.gmailThreadId = :gmailThreadId
            """)
    boolean existsByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Message m SET m.deletedAt = :deletedAt WHERE m.thread.id IN (SELECT t.id FROM Thread t WHERE t.mailAccount.id = :mailAccountId AND t.gmailThreadId = :gmailThreadId) AND m.deletedAt IS NULL")
    int bulkSoftDeleteByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Message m SET m.deletedAt = NULL WHERE m.thread.id IN (SELECT t.id FROM Thread t WHERE t.mailAccount.id = :mailAccountId AND t.gmailThreadId = :gmailThreadId) AND m.deletedAt IS NOT NULL")
    int bulkRestoreByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @EntityGraph(attributePaths = {"messageLabels", "messageLabels.label", "thread", "thread.mailAccount"})
    @Query("""
            SELECT DISTINCT m FROM Message m
            WHERE m.thread.mailAccount.user.id = :userId
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
            """)
    List<Message> findAllByUserIdAndDeletedAtIsNullWithLabels(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"attachments"})
    @Query("""
            SELECT DISTINCT m FROM Message m
            WHERE m.thread.mailAccount.user.id = :userId
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
            """)
    List<Message> findAllByUserIdAndDeletedAtIsNullWithAttachments(@Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT tl.thread.id AS threadId,
                            tl.label.id  AS labelId,
                            tl.label.name AS labelName,
                            tl.label.colorCode AS labelColorCode
            FROM ThreadLabel tl
            WHERE tl.thread.id IN :threadIds
              AND tl.deletedAt IS NULL
              AND tl.label.deletedAt IS NULL
            """)
    List<ThreadLabelProjection> findLabelsByThreadIdIn(@Param("threadIds") List<UUID> threadIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MessageLabel ml WHERE ml.label.id = :labelId")
    int deleteMessageLabelsByLabelId(@Param("labelId") UUID labelId);

    @Query("""
            SELECT ml.message.id AS messageId,
                   ml.label.id AS labelId,
                   ml.label.name AS labelName,
                   ml.label.colorCode AS colorCode
            FROM MessageLabel ml
            WHERE ml.message.id IN :messageIds
              AND ml.label.deletedAt IS NULL
            """)
    List<MessageLabelProjection> findMessageLabelsByMessageIdIn(@Param("messageIds") List<UUID> messageIds);

    @EntityGraph(attributePaths = {"messageLabels", "messageLabels.label", "thread", "thread.mailAccount"})
    @Query("SELECT m FROM Message m WHERE m.id = :id AND m.deletedAt IS NULL AND m.thread.deletedAt IS NULL AND m.thread.mailAccount.deletedAt IS NULL")
    Optional<Message> findByIdWithLabelsAndDeletedAtIsNull(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT m.thread.id FROM Message m
            WHERE m.thread.mailAccount.user.id = :userId
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
            """)
    List<UUID> findActiveThreadIdsByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"messageLabels", "messageLabels.label", "thread", "thread.mailAccount"})
    @Query("SELECT DISTINCT m FROM Message m WHERE m.thread.id IN :threadIds AND m.deletedAt IS NULL AND m.thread.deletedAt IS NULL AND m.thread.mailAccount.deletedAt IS NULL")
    List<Message> findActiveMessagesWithLabelsByThreadIdIn(@Param("threadIds") List<UUID> threadIds);

    @EntityGraph(attributePaths = {"messageLabels", "messageLabels.label", "thread", "thread.mailAccount"})
    @Query("""
            SELECT DISTINCT m FROM Message m
            WHERE m.thread.mailAccount.user.id = :userId
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
            ORDER BY m.id ASC
            """)
    List<Message> findActiveMessagesWithLabelsByUserIdPaged(@Param("userId") UUID userId, Pageable pageable);
}
