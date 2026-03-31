package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;

import java.util.UUID;

public record MailAccountResponse(
        UUID id,
        UUID accountId,
        MailProvider provider,
        String emailAddress,
        boolean active,
        String syncHistoryId
) {
    public static MailAccountResponse from(MailAccount mailAccount) {
        return new MailAccountResponse(
                mailAccount.getId(),
                mailAccount.getAccountId(),
                mailAccount.getProvider(),
                mailAccount.getEmailAddress(),
                mailAccount.isActive(),
                mailAccount.getSyncHistoryId()
        );
    }
}
