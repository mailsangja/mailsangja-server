package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;

import java.util.UUID;

public record InitialMailSyncCommand(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress
) {

    public static InitialMailSyncCommand from(MailAccount mailAccount) {
        return new InitialMailSyncCommand(
                mailAccount.getId(),
                mailAccount.getUser().getId(),
                mailAccount.getProvider().name(),
                mailAccount.getEmailAddress()
        );
    }

    public boolean isGoogleMailAccount() {
        return MailProvider.GMAIL.name().equals(provider);
    }
}
