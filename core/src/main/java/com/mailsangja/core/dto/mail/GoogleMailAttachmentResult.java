package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.AttachmentDisposition;

public record GoogleMailAttachmentResult(
        String gmailAttachmentId,
        String filename,
        String mimeType,
        String contentId,
        AttachmentDisposition disposition,
        Integer size
) {
}
