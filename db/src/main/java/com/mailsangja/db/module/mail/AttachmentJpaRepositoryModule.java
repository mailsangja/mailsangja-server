package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachmentJpaRepositoryModule extends JpaRepository<Attachment, UUID> {
    List<Attachment> findAllByMessageId(UUID messageId);
}
