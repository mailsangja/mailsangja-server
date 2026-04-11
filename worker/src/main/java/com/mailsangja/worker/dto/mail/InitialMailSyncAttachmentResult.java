package com.mailsangja.worker.dto.mail;

public record InitialMailSyncAttachmentResult(
        String gmailAttachmentId,
        String filename,
        String mimeType,
        Integer size
) {
}
