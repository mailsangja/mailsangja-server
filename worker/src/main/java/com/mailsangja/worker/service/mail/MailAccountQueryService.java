package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountQueryService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;

    public MailAccount findActiveGoogleMailAccountByEmailAddress(String emailAddress) {
        MailAccount mailAccount = mailAccountRepositoryPort.findByProviderAndEmailAddress(MailProvider.GMAIL, emailAddress)
                .orElseThrow(() -> new MailPushException(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        validateActiveMailAccount(mailAccount);
        return mailAccount;
    }

    public MailAccount findActiveMailAccountById(UUID id) {
        MailAccount mailAccount = mailAccountRepositoryPort.findByIdAndActive(id, true)
                .orElseThrow(() -> new MailPushException(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        validateActiveMailAccount(mailAccount);
        return mailAccount;
    }

    private void validateActiveMailAccount(MailAccount mailAccount) {
        if (!mailAccount.isActive()
                || mailAccount.isDeleted()
                || mailAccount.getProvider() != MailProvider.GMAIL
                || isBlank(mailAccount.getAccessToken())) {
            throw new MailPushException(MailPushErrorCode.INVALID_MAIL_ACCOUNT_STATE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
