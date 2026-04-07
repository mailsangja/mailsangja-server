package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.Attachment;

import java.util.List;
import java.util.UUID;

public interface AttachmentRepositoryPort {
    Attachment save(Attachment attachment);
    List<Attachment> findAllByMessageId(UUID messageId);
}
