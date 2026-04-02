package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.module.mail.MailAccountJpaRepositoryModule;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MailAccountRepositoryAdapter implements MailAccountRepositoryPort {

    private final MailAccountJpaRepositoryModule mailAccountJpaRepositoryModule;

    @Override
    public MailAccount save(MailAccount mailAccount) {
        return mailAccountJpaRepositoryModule.save(mailAccount);
    }

    @Override
    public Optional<MailAccount> findById(UUID id) {
        return mailAccountJpaRepositoryModule.findById(id);
    }

    @Override
    public Optional<MailAccount> findByIdAndActive(UUID id, boolean active) {
        return mailAccountJpaRepositoryModule.findByIdAndActive(id, active);
    }

    @Override
    public Optional<MailAccount> findByEmailAddress(String emailAddress) {
        return mailAccountJpaRepositoryModule.findByEmailAddress(emailAddress);
    }

    @Override
    public Optional<MailAccount> findByUserIdAndProvider(UUID userId, MailProvider provider) {
        return mailAccountJpaRepositoryModule.findByUserIdAndProvider(userId, provider);
    }

    @Override
    public Optional<MailAccount> findByUserIdAndProviderAndEmailAddress(UUID userId, MailProvider provider, String emailAddress) {
        return mailAccountJpaRepositoryModule.findByUserIdAndProviderAndEmailAddress(userId, provider, emailAddress);
    }

    @Override
    public Optional<MailAccount> findByProviderAndEmailAddress(MailProvider provider, String emailAddress) {
        return mailAccountJpaRepositoryModule.findByProviderAndEmailAddress(provider, emailAddress);
    }

    @Override
    public List<MailAccount> findAllByUserIdAndDeletedAtIsNull(UUID userId) {
        return mailAccountJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNull(userId);
    }
}
