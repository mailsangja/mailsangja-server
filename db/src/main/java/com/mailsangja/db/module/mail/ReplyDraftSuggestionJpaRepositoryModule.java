package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReplyDraftSuggestionJpaRepositoryModule extends JpaRepository<ReplyDraftSuggestion, UUID> {
    List<ReplyDraftSuggestion> findAllByMessageIdAndDeletedAtIsNull(UUID messageId);
    Optional<ReplyDraftSuggestion> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByMessageId(UUID messageId);

    @Modifying
    @Query(value = """
            WITH locked_message AS (
                SELECT id
                FROM messages
                WHERE id = :messageId
                  AND deleted_at IS NULL
                FOR UPDATE
            )
            INSERT INTO reply_draft_suggestions (id, message_id, type, subject, body, created_at, modified_at)
            SELECT :id, lm.id, :type, :subject, :body, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM locked_message lm
            WHERE (
                SELECT COUNT(*)
                FROM reply_draft_suggestions r
                WHERE r.message_id = lm.id
                  AND r.deleted_at IS NULL
            ) < :limit
            """, nativeQuery = true)
    int insertIfActiveCountBelowLimit(
            @Param("id") String id,
            @Param("messageId") String messageId,
            @Param("type") String type,
            @Param("subject") String subject,
            @Param("body") String body,
            @Param("limit") int limit
    );
}
