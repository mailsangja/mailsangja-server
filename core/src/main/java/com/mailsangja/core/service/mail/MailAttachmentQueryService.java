package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAttachmentQueryService {

    private final AttachmentRepositoryPort attachmentRepositoryPort;

    public Attachment findById(UUID attachmentId) {
        Attachment attachment = attachmentRepositoryPort.findByIdAndDeletedAtIsNull(attachmentId)
                .orElseThrow(() -> new InboxException(InboxErrorCode.ATTACHMENT_NOT_FOUND));
        validateAttachment(attachment);
        return attachment;
    }

    private void validateAttachment(Attachment attachment) {
        if (attachment == null
                || attachment.getMessage() == null
                || attachment.getMessage().getThread() == null
                || attachment.getMessage().getThread().getMailAccount() == null) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_SOURCE_INVALID);
        }
    }
}
