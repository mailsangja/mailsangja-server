package com.mailsangja.core.dto.mail;

public record GoogleMailAttachmentResponse(
        String attachmentId,
        Integer size,
        String data
) {
}
