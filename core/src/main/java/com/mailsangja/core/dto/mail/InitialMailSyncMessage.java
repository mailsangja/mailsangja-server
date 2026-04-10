package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;

import java.util.UUID;

public record InitialMailSyncMessage(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress
) {

    public static InitialMailSyncMessage from(MailAccount mailAccount) {
        return new InitialMailSyncMessage(
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
