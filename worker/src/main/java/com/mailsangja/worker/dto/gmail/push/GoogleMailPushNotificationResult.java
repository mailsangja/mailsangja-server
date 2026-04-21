package com.mailsangja.worker.dto.gmail.push;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;

public record GoogleMailPushNotificationResult(
        String emailAddress,
        String historyId
) {
    public GoogleMailPushNotificationResult {
        if (emailAddress == null || emailAddress.isBlank()) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
        }
        if (historyId == null || historyId.isBlank()) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
        }
    }
}
