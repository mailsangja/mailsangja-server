package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.Attachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepositoryPort {
    Attachment save(Attachment attachment);
    Optional<Attachment> findByIdAndDeletedAtIsNull(UUID id);
    List<Attachment> findAllByMessageIdAndDeletedAtIsNull(UUID messageId);
}
