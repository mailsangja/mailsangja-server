package com.mailsangja.worker.dto.mail.sync;

import com.mailsangja.db.entity.mail.AttachmentDisposition;

public record InitialMailSyncAttachmentResult(
        String gmailAttachmentId,
        String filename,
        String mimeType,
        String contentId,
        AttachmentDisposition disposition,
        Integer size
) {
}
