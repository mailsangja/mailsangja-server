package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountQueryService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MailAccountRepositoryPort mailAccountRepositoryPort;

    public MailAccount findById(UUID id) {
        return mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND));
    }

    public MailAccount findActiveById(UUID id) {
        return mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(id, true)
                .orElseThrow(() -> new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND));
    }

    public MailAccount findActiveByUserIdAndEmailAddress(UUID userId, String emailAddress) {
        return mailAccountRepositoryPort.findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(userId, emailAddress, true)
                .orElseThrow(() -> new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND));
    }

    public Optional<MailAccount> findByUserIdAndProviderAndEmailAddress(UUID userId, MailProvider provider, String emailAddress) {
        return mailAccountRepositoryPort.findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(userId, provider, emailAddress);
    }

    public Optional<MailAccount> findByProviderAndEmailAddress(MailProvider provider, String emailAddress) {
        return mailAccountRepositoryPort.findByProviderAndEmailAddressAndDeletedAtIsNull(provider, emailAddress);
    }

    public List<MailAccount> findAllByUserId(UUID userId) {
        return mailAccountRepositoryPort.findAllByUserIdAndDeletedAtIsNull(userId);
    }

    public List<MailAccount> findAllActiveByUserId(UUID userId) {
        return mailAccountRepositoryPort.findAllByUserIdAndActiveAndDeletedAtIsNull(userId, true);
    }

    public boolean existsOtherActiveGmailAccountByEmailAddress(UUID excludeUserId, String emailAddress) {
        return mailAccountRepositoryPort.existsByProviderAndEmailAddressAndUserIdNotAndDeletedAtIsNull(
                MailProvider.GMAIL, emailAddress, excludeUserId
        );
    }

    public LocalDateTime getKstNow() {
        return LocalDateTime.now(KST_ZONE_ID);
    }
}
