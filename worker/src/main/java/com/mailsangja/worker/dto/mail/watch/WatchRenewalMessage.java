package com.mailsangja.worker.dto.mail.watch;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;

import java.util.UUID;

public record WatchRenewalMessage(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress
) {

    public WatchRenewalMessage {
        if (mailAccountId == null
                || userId == null
                || provider == null || provider.isBlank()
                || emailAddress == null || emailAddress.isBlank()
                || !MailProvider.GMAIL.name().equals(provider)) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_COMMAND);
        }
    }

    public static WatchRenewalMessage from(MailAccount mailAccount) {
        return new WatchRenewalMessage(
                mailAccount.getId(),
                mailAccount.getUser().getId(),
                mailAccount.getProvider().name(),
                mailAccount.getEmailAddress()
        );
    }
}
