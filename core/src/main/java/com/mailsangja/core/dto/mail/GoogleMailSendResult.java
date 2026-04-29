package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;

public record GoogleMailSendResult(
        String gmailMessageId,
        String gmailThreadId
) {
    public static void validate(GoogleMailSendResult result) {
        if (result == null
                || isBlank(result.gmailMessageId())
                || isBlank(result.gmailThreadId())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_SEND_RESULT_INVALID);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
