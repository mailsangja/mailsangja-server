package com.mailsangja.core.dto.mail;

public record MailAttachmentDownloadResult(
        String filename,
        String mimeType,
        byte[] bytes
) {
}
