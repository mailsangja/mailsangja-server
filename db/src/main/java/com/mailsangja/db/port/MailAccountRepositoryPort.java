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
    List<MailAccount> findAllByAccountId(UUID accountId);
}
