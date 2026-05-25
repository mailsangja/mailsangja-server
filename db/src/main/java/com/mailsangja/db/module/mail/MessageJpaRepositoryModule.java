package com.mailsangja.db.module.mail;

import com.mailsangja.db.dto.MessageLabelProjection;
import com.mailsangja.db.dto.ThreadMessageLabelProjection;
import com.mailsangja.db.entity.mail.Direction;
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

    @Query("""
            SELECT COUNT(m) > 0
            FROM Message m
            WHERE m.thread.mailAccount.id = :mailAccountId
              AND m.thread.gmailThreadId = :gmailThreadId
              AND m.direction = :direction
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
            """)
    boolean existsByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId,
            @Param("direction") Direction direction
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

    @EntityGraph(attributePaths = {"attachments"})
    @Query("""
            SELECT m
            FROM Message m
            WHERE m.thread.mailAccount.id = :mailAccountId
              AND m.thread.gmailThreadId = :gmailThreadId
              AND m.deletedAt IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM MessageLabel ml
                  WHERE ml.message.id = m.id
                    AND ml.deletedAt IS NULL
                    AND ml.label.deletedAt IS NULL
                    AND ml.label.isSensitive = true
              )
            ORDER BY m.sentAt ASC
            """)
    List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndSensitiveLabelsExcluded(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount", "thread.mailAccount.user"})
    Optional<Message> findById(UUID id);

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount", "thread.mailAccount.user"})
    @Query("""
            SELECT m
            FROM Message m
            WHERE m.id = :messageId
              AND NOT EXISTS (
                  SELECT 1
                  FROM MessageLabel ml
                  WHERE ml.message.id = m.id
                    AND ml.deletedAt IS NULL
                    AND ml.label.deletedAt IS NULL
                    AND ml.label.isSensitive = true
              )
            """)
    Optional<Message> findByIdIncludingDeletedAndSensitiveLabelsExcluded(@Param("messageId") UUID messageId);

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
    @Query("""
            SELECT m FROM Message m
            WHERE m.thread.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND m.deletedAt IS NOT NULL
              AND (
                :markerId IS NULL
                OR m.deletedAt < (SELECT mm.deletedAt FROM Message mm WHERE mm.id = :markerId)
                OR (
                  m.deletedAt = (SELECT mm.deletedAt FROM Message mm WHERE mm.id = :markerId)
                  AND m.id < :markerId
                )
              )
            ORDER BY m.deletedAt DESC, m.id DESC
            """)
    Slice<Message> findDeletedByUserId(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount"})
    @Query("""
            SELECT m FROM Message m
            WHERE m.thread.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND m.deletedAt IS NOT NULL
              AND (:read IS NULL OR m.read = :read)
              AND (
                :markerId IS NULL
                OR m.deletedAt < (SELECT mm.deletedAt FROM Message mm WHERE mm.id = :markerId)
                OR (
                  m.deletedAt = (SELECT mm.deletedAt FROM Message mm WHERE mm.id = :markerId)
                  AND m.id < :markerId
                )
              )
            ORDER BY m.deletedAt DESC, m.id DESC
            """)
    Slice<Message> findDeletedByUserIdAndReadFilter(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            @Param("read") Boolean read,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount"})
    @Query("""
            SELECT DISTINCT m FROM Message m
            JOIN m.messageLabels ml
            WHERE m.thread.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND m.deletedAt IS NOT NULL
              AND (:read IS NULL OR m.read = :read)
              AND ml.deletedAt IS NULL
              AND ml.label.id IN :labelIds
              AND ml.label.deletedAt IS NULL
              AND (
                :markerId IS NULL
                OR m.deletedAt < (SELECT mm.deletedAt FROM Message mm WHERE mm.id = :markerId)
                OR (
                  m.deletedAt = (SELECT mm.deletedAt FROM Message mm WHERE mm.id = :markerId)
                  AND m.id < :markerId
                )
              )
            ORDER BY m.deletedAt DESC, m.id DESC
            """)
    Slice<Message> findDeletedByUserIdAndLabelIdsAndReadFilter(
            @Param("userId") UUID userId,
            @Param("markerId") UUID markerId,
            @Param("labelIds") List<UUID> labelIds,
            @Param("read") Boolean read,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.thread.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND m.deletedAt IS NOT NULL
              AND m.read = false
            """)
    long countUnreadDeletedByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.thread.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND m.deletedAt IS NOT NULL
              AND (:read IS NULL OR m.read = :read)
            """)
    long countDeletedByUserIdAndReadFilter(
            @Param("userId") UUID userId,
            @Param("read") Boolean read
    );

    @Query("""
            SELECT COUNT(DISTINCT m) FROM Message m
            JOIN m.messageLabels ml
            WHERE m.thread.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND m.deletedAt IS NOT NULL
              AND m.read = false
              AND ml.deletedAt IS NULL
              AND ml.label.id IN :labelIds
              AND ml.label.deletedAt IS NULL
            """)
    long countUnreadDeletedByUserIdAndLabelIds(
            @Param("userId") UUID userId,
            @Param("labelIds") List<UUID> labelIds
    );

    @Query("""
            SELECT COUNT(DISTINCT m) FROM Message m
            JOIN m.messageLabels ml
            WHERE m.thread.mailAccount.id IN (
                SELECT ma.id FROM MailAccount ma
                WHERE ma.user.id = :userId AND ma.deletedAt IS NULL
            )
              AND m.deletedAt IS NOT NULL
              AND (:read IS NULL OR m.read = :read)
              AND ml.deletedAt IS NULL
              AND ml.label.id IN :labelIds
              AND ml.label.deletedAt IS NULL
            """)
    long countDeletedByUserIdAndLabelIdsAndReadFilter(
            @Param("userId") UUID userId,
            @Param("labelIds") List<UUID> labelIds,
            @Param("read") Boolean read
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
            SELECT DISTINCT ml.message.thread.id AS threadId,
                            ml.label.id  AS labelId,
                            ml.label.name AS labelName,
                            ml.label.colorCode AS labelColorCode,
                            ml.label.isSensitive AS labelIsSensitive
            FROM MessageLabel ml
            WHERE ml.message.thread.id IN :threadIds
              AND ml.deletedAt IS NULL
              AND ml.label.deletedAt IS NULL
            """)
    List<ThreadMessageLabelProjection> findLabelsByThreadIdIn(@Param("threadIds") List<UUID> threadIds);

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

    @Query("""
            SELECT m
            FROM Message m
            WHERE m.thread.mailAccount.user.id = :userId
              AND m.thread.mailAccount.id = :mailAccountId
              AND m.direction = :direction
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM MessageLabel ml
                  WHERE ml.message.id = m.id
                    AND ml.deletedAt IS NULL
                    AND ml.label.deletedAt IS NULL
                    AND ml.label.isSensitive = true
              )
            ORDER BY m.sentAt DESC, m.id DESC
            """)
    List<Message> findRecentByUserIdAndMailAccountIdAndDirection(
            @Param("userId") UUID userId,
            @Param("mailAccountId") UUID mailAccountId,
            @Param("direction") Direction direction,
            Pageable pageable
    );

    @Query(value = """
            SELECT m.*
            FROM messages m
            JOIN threads t ON m.thread_id = t.id
            JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId
              AND ma.id = :mailAccountId
              AND m.direction = 'OUTBOUND'
              AND m.deleted_at IS NULL
              AND t.deleted_at IS NULL
              AND ma.deleted_at IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM message_labels ml
                  JOIN labels l ON l.id = ml.label_id
                  WHERE ml.message_id = m.id
                    AND ml.deleted_at IS NULL
                    AND l.deleted_at IS NULL
                    AND l.is_sensitive = true
              )
              AND (
                  LOWER(COALESCE(m.subject, '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                  OR LOWER(COALESCE(m.body_text, '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                  OR LOWER(COALESCE(m.from_name, '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                  OR LOWER(COALESCE(CAST(m.to_names AS text), '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                  OR LOWER(COALESCE(CAST(m.to_addresses AS text), '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                  OR LOWER(COALESCE(CAST(m.cc_names AS text), '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                  OR LOWER(COALESCE(CAST(m.cc_addresses AS text), '')) LIKE LOWER(CONCAT('%', :hint, '%'))
              )
            ORDER BY m.sent_at DESC NULLS LAST, m.id DESC
            """, nativeQuery = true)
    List<Message> findWrittenByUserIdAndMailAccountIdAndHint(
            @Param("userId") String userId,
            @Param("mailAccountId") String mailAccountId,
            @Param("hint") String hint,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM Message m
            WHERE m.thread.mailAccount.user.id = :userId
              AND m.direction = :direction
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM MessageLabel ml
                  WHERE ml.message.id = m.id
                    AND ml.deletedAt IS NULL
                    AND ml.label.deletedAt IS NULL
                    AND ml.label.isSensitive = true
              )
            ORDER BY m.sentAt DESC, m.id DESC
            """)
    List<Message> findRecentByUserIdAndDirection(
            @Param("userId") UUID userId,
            @Param("direction") Direction direction,
            Pageable pageable
    );

    @Query(value = """
            SELECT m.*
            FROM messages m
            JOIN threads t ON m.thread_id = t.id
            JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId
              AND ma.id = :mailAccountId
              AND m.deleted_at IS NULL
              AND t.deleted_at IS NULL
              AND ma.deleted_at IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM message_labels ml
                  JOIN labels l ON l.id = ml.label_id
                  WHERE ml.message_id = m.id
                    AND ml.deleted_at IS NULL
                    AND l.deleted_at IS NULL
                    AND l.is_sensitive = true
              )
              AND (
                  (
                      m.direction = 'OUTBOUND'
                      AND (
                          LOWER(COALESCE(CAST(m.to_addresses AS text), '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                          OR LOWER(COALESCE(CAST(m.to_names AS text), '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                          OR LOWER(COALESCE(CAST(m.cc_addresses AS text), '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                          OR LOWER(COALESCE(CAST(m.cc_names AS text), '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                      )
                  )
                  OR (
                      m.direction = 'INBOUND'
                      AND (
                          LOWER(COALESCE(m.from_address, '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                          OR LOWER(COALESCE(m.from_name, '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                          OR LOWER(COALESCE(m.reply_to_address, '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                          OR LOWER(COALESCE(m.reply_to_name, '')) LIKE LOWER(CONCAT('%', :hint, '%'))
                      )
                  )
              )
            ORDER BY m.sent_at DESC NULLS LAST, m.id DESC
            """, nativeQuery = true)
    List<Message> findRecipientHistoryByUserIdAndMailAccountIdAndHint(
            @Param("userId") String userId,
            @Param("mailAccountId") String mailAccountId,
            @Param("hint") String hint,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount"})
    @Query("SELECT m FROM Message m WHERE m.id IN :ids")
    List<Message> findAllByIdInWithThread(@Param("ids") List<UUID> ids);

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount"})
    @Query("""
            SELECT m
            FROM Message m
            WHERE m.id IN :messageIds
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM MessageLabel ml
                  WHERE ml.message.id = m.id
                    AND ml.deletedAt IS NULL
                    AND ml.label.deletedAt IS NULL
                    AND ml.label.isSensitive = true
              )
            """)
    List<Message> findActiveByIdIn(@Param("messageIds") List<UUID> messageIds);

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount"})
    @Query("""
            SELECT m
            FROM Message m
            WHERE m.thread.mailAccount.id = (
                SELECT target.thread.mailAccount.id FROM Message target WHERE target.id = :replyMessageId
            )
              AND m.thread.gmailThreadId = (
                SELECT target.thread.gmailThreadId FROM Message target WHERE target.id = :replyMessageId
            )
              AND m.deletedAt IS NULL
              AND m.thread.deletedAt IS NULL
              AND m.thread.mailAccount.deletedAt IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM MessageLabel ml
                  WHERE ml.message.id = m.id
                    AND ml.deletedAt IS NULL
                    AND ml.label.deletedAt IS NULL
                    AND ml.label.isSensitive = true
              )
            ORDER BY m.sentAt ASC, m.id ASC
            """)
    List<Message> findThreadContextByReplyMessageId(@Param("replyMessageId") UUID replyMessageId);
}
