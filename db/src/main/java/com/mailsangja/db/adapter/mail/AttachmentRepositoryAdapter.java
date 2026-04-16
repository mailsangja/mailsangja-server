package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.module.mail.AttachmentJpaRepositoryModule;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AttachmentRepositoryAdapter implements AttachmentRepositoryPort {

    private final AttachmentJpaRepositoryModule attachmentJpaRepositoryModule;

    @Override
    public Attachment save(Attachment attachment) {
        return attachmentJpaRepositoryModule.save(attachment);
    }

    @Override
    public Optional<Attachment> findByIdAndDeletedAtIsNull(UUID id) {
        return attachmentJpaRepositoryModule.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Attachment> findAllByMessageIdAndDeletedAtIsNull(UUID messageId) {
        return attachmentJpaRepositoryModule.findAllByMessageIdAndDeletedAtIsNull(messageId);
    }
}
