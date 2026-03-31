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
    public Optional<MailAccount> findByEmailAddress(String emailAddress) {
        return mailAccountJpaRepositoryModule.findByEmailAddress(emailAddress);
    }

    @Override
    public Optional<MailAccount> findByAccountIdAndProvider(UUID accountId, MailProvider provider) {
        return mailAccountJpaRepositoryModule.findByAccountIdAndProvider(accountId, provider);
    }

    @Override
    public List<MailAccount> findAllByAccountId(UUID accountId) {
        return mailAccountJpaRepositoryModule.findAllByAccountId(accountId);
    }
}
