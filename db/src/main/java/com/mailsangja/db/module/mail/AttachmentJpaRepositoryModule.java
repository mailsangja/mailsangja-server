package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AttachmentJpaRepositoryModule extends JpaRepository<Attachment, UUID> {
    @EntityGraph(attributePaths = {"message", "message.thread", "message.thread.mailAccount"})
    Optional<Attachment> findByIdAndDeletedAtIsNull(UUID id);

    List<Attachment> findAllByMessageIdAndDeletedAtIsNull(UUID messageId);

    @Query("SELECT DISTINCT a.message.id FROM Attachment a WHERE a.message.id IN :messageIds AND a.deletedAt IS NULL")
    Set<UUID> findMessageIdsHavingAttachments(@Param("messageIds") Set<UUID> messageIds);

    @EntityGraph(attributePaths = {"message"})
    @Query("SELECT a FROM Attachment a WHERE a.message.id IN :messageIds AND a.deletedAt IS NULL")
    List<Attachment> findAllByMessageIdIn(@Param("messageIds") List<UUID> messageIds);
}
