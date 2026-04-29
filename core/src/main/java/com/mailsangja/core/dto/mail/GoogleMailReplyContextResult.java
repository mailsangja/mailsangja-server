package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.db.entity.mail.Message;

public record GoogleMailReplyContextResult(
        String gmailThreadId,
        String parentRfcMessageId,
        String referencesHeader,
        String subject
) {
    public static GoogleMailReplyContextResult from(Message replyTargetMessage) {
        if (replyTargetMessage.getThread() == null
                || isBlank(replyTargetMessage.getThread().getGmailThreadId())
                || isBlank(replyTargetMessage.getRfcMessageId())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }

        return new GoogleMailReplyContextResult(
                replyTargetMessage.getThread().getGmailThreadId(),
                replyTargetMessage.getRfcMessageId(),
                replyTargetMessage.getReferencesHeader(),
                replyTargetMessage.getSubject()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
