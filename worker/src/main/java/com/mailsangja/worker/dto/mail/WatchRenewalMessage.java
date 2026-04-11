package com.mailsangja.worker.dto.mail;

import com.mailsangja.db.entity.mail.MailAccount;

import java.util.UUID;

public record WatchRenewalMessage(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress
) {

    public static WatchRenewalMessage from(MailAccount mailAccount) {
        return new WatchRenewalMessage(
                mailAccount.getId(),
                mailAccount.getUser().getId(),
                mailAccount.getProvider().name(),
                mailAccount.getEmailAddress()
        );
    }
}
