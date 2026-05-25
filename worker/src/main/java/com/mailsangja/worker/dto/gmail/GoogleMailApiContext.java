package com.mailsangja.worker.dto.gmail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;

import java.util.Locale;

public record GoogleMailApiContext(
        String accessToken,
        String accountKey
) {
    public GoogleMailApiContext {
        if (isBlank(accessToken) || isBlank(accountKey)) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }
        accountKey = accountKey.strip().toLowerCase(Locale.ROOT);
    }

    public static GoogleMailApiContext from(MailAccount mailAccount) {
        if (mailAccount == null) {
            throw new MailPushException(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND);
        }
        return new GoogleMailApiContext(mailAccount.getAccessToken(), mailAccount.getEmailAddress());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
