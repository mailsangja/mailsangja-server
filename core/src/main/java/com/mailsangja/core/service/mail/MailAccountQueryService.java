package com.mailsangja.core.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountQueryService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;

    public Optional<MailAccount> findByAccountIdAndProviderAndEmailAddress(
            UUID accountId,
            MailProvider provider,
            String emailAddress
    ) {
        return mailAccountRepositoryPort.findByAccountIdAndProviderAndEmailAddress(accountId, provider, emailAddress);
    }

    public Optional<MailAccount> findByProviderAndEmailAddress(MailProvider provider, String emailAddress) {
        return mailAccountRepositoryPort.findByProviderAndEmailAddress(provider, emailAddress);
    }
}
