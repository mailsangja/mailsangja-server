package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountQueryService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MailAccountRepositoryPort mailAccountRepositoryPort;

    public MailAccount findSyncableGoogleMailAccountByEmailAddress(String emailAddress) {
        MailAccount mailAccount = mailAccountRepositoryPort.findByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider.GMAIL, emailAddress)
                .orElseThrow(() -> new MailPushException(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        validateSyncableMailAccount(mailAccount);
        return mailAccount;
    }

    public MailAccount findSyncableMailAccountById(UUID id) {
        MailAccount mailAccount = mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new MailPushException(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        validateSyncableMailAccount(mailAccount);
        return mailAccount;
    }

    public List<MailAccount> findRenewalTargetGmailAccounts(LocalDateTime renewalWindowThreshold, int limit) {
        if (renewalWindowThreshold == null || limit <= 0) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST);
        }

        return mailAccountRepositoryPort.findRenewalTargetGmailAccounts(MailProvider.GMAIL, renewalWindowThreshold, limit);
    }

    public LocalDateTime getKstNow() {
        return LocalDateTime.now(KST_ZONE_ID);
    }

    private void validateSyncableMailAccount(MailAccount mailAccount) {
        if (mailAccount.isDeleted()
                || mailAccount.getProvider() != MailProvider.GMAIL
                || isBlank(mailAccount.getAccessToken())) {
            throw new MailPushException(MailPushErrorCode.INVALID_MAIL_ACCOUNT_STATE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
