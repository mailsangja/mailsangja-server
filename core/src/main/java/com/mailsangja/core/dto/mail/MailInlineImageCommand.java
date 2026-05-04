package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public record MailInlineImageCommand(
        String cid,
        String filename,
        String contentType,
        byte[] bytes
) {

    public static MailInlineImageCommand from(MultipartFile inlineImage, String cid) {
        try {
            return new MailInlineImageCommand(
                    cid,
                    inlineImage.getOriginalFilename(),
                    inlineImage.getContentType(),
                    inlineImage.getBytes()
            );
        } catch (IOException e) {
            throw new MailSendException(MailSendErrorCode.ATTACHMENT_READ_FAILED);
        }
    }
}
