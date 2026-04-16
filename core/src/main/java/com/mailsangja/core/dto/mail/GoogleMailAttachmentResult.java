package com.mailsangja.core.dto.mail;

public record GoogleMailAttachmentResult(
        String gmailAttachmentId,
        String filename,
        String mimeType,
        Integer size
) {
}
