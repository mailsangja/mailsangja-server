package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentJpaRepositoryModule extends JpaRepository<Attachment, UUID> {
    @EntityGraph(attributePaths = {"message", "message.thread", "message.thread.mailAccount"})
    Optional<Attachment> findByIdAndDeletedAtIsNull(UUID id);

    List<Attachment> findAllByMessageId(UUID messageId);
}
