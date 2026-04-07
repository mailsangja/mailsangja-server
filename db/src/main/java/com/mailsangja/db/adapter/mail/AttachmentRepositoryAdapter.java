package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.module.mail.AttachmentJpaRepositoryModule;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public List<Attachment> findAllByMessageId(UUID messageId) {
        return attachmentJpaRepositoryModule.findAllByMessageId(messageId);
    }
}
