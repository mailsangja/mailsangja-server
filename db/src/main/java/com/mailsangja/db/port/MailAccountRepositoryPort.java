package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailAccountRepositoryPort {
    MailAccount save(MailAccount mailAccount);
    Optional<MailAccount> findById(UUID id);
    Optional<MailAccount> findByEmailAddress(String emailAddress);
    Optional<MailAccount> findByAccountIdAndProvider(UUID accountId, MailProvider provider);
    Optional<MailAccount> findByAccountIdAndProviderAndEmailAddress(UUID accountId, MailProvider provider, String emailAddress);
    Optional<MailAccount> findByProviderAndEmailAddress(MailProvider provider, String emailAddress);
    List<MailAccount> findAllByAccountId(UUID accountId);
}
