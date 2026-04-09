package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountQueryService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;

    public MailAccount findById(UUID id) {
        return mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND));
    }

    public MailAccount findActiveById(UUID id) {
        return mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(id, true)
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
}
