package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public record MailAttachmentCommand(
        String filename,
        String contentType,
        byte[] bytes
) {

    public static MailAttachmentCommand from(MultipartFile attachment) {
        try {
            return new MailAttachmentCommand(
                    attachment.getOriginalFilename(),
                    attachment.getContentType(),
                    attachment.getBytes()
            );
        } catch (IOException e) {
            throw new MailSendException(MailSendErrorCode.ATTACHMENT_READ_FAILED);
        }
    }
}
