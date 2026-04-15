package com.mailsangja.worker.dto.mail.sync;

public record InitialMailSyncAttachmentResult(
        String gmailAttachmentId,
        String filename,
        String mimeType,
        Integer size
) {
}
