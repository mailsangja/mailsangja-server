package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;

import java.time.LocalDateTime;
import java.util.List;

public record GoogleMailMessageResult(
        String gmailMessageId,
        String gmailThreadId,
        String historyId,
        String rfcMessageId,
        String referencesHeader,
        String inReplyToHeader,
        String replyToAddress,
        String replyToName,
        String subject,
        String fromAddress,
        String fromName,
        List<String> toAddresses,
        List<String> toNames,
        List<String> ccAddresses,
        List<String> ccNames,
        String snippet,
        LocalDateTime sentAt,
        String bodyText,
        String bodyHtml,
        List<GoogleMailAttachmentResult> attachments
) {
    public static void validateAgainst(GoogleMailMessageResult messageResult, GoogleMailSendResult sendResult) {
        GoogleMailSendResult.validate(sendResult);
        if (messageResult == null
                || isBlank(messageResult.gmailMessageId())
                || isBlank(messageResult.gmailThreadId())
                || !sendResult.gmailMessageId().equals(messageResult.gmailMessageId())
                || !sendResult.gmailThreadId().equals(messageResult.gmailThreadId())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
