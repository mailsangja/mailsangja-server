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
    public Optional<MailAccount> findByIdAndDeletedAtIsNull(UUID id) {
        return mailAccountJpaRepositoryModule.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<MailAccount> findByIdAndActiveAndDeletedAtIsNull(UUID id, boolean active) {
        return mailAccountJpaRepositoryModule.findByIdAndActiveAndDeletedAtIsNull(id, active);
    }

    @Override
    public Optional<MailAccount> findByEmailAddressAndDeletedAtIsNull(String emailAddress) {
        return mailAccountJpaRepositoryModule.findByEmailAddressAndDeletedAtIsNull(emailAddress);
    }

    @Override
    public Optional<MailAccount> findByUserIdAndProviderAndDeletedAtIsNull(UUID userId, MailProvider provider) {
        return mailAccountJpaRepositoryModule.findByUserIdAndProviderAndDeletedAtIsNull(userId, provider);
    }

    @Override
    public Optional<MailAccount> findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(UUID userId, MailProvider provider, String emailAddress) {
        return mailAccountJpaRepositoryModule.findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(userId, provider, emailAddress);
    }

    @Override
    public Optional<MailAccount> findByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider provider, String emailAddress) {
        return mailAccountJpaRepositoryModule.findByProviderAndEmailAddressAndDeletedAtIsNull(provider, emailAddress);
    }

    @Override
    public List<MailAccount> findAllByUserIdAndDeletedAtIsNull(UUID userId) {
        return mailAccountJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public List<MailAccount> findAllByUserIdAndActiveAndDeletedAtIsNull(UUID userId, boolean active) {
        return mailAccountJpaRepositoryModule.findAllByUserIdAndActiveAndDeletedAtIsNull(userId, active);
    }
}
