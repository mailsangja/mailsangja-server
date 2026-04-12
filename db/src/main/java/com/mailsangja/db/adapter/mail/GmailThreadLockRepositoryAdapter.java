package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.GmailThreadLock;
import com.mailsangja.db.module.mail.GmailThreadLockJpaRepositoryModule;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GmailThreadLockRepositoryAdapter implements GmailThreadLockRepositoryPort {

    private final GmailThreadLockJpaRepositoryModule gmailThreadLockJpaRepositoryModule;

    @Override
    public GmailThreadLock save(GmailThreadLock gmailThreadLock) {
        return gmailThreadLockJpaRepositoryModule.saveAndFlush(gmailThreadLock);
    }

    @Override
    public Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
            UUID mailAccountId,
            String gmailThreadId
    ) {
        return gmailThreadLockJpaRepositoryModule.findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccountId,
                gmailThreadId
        );
    }

    @Override
    public Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(
            UUID mailAccountId,
            String gmailThreadId
    ) {
        return gmailThreadLockJpaRepositoryModule.findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(
                mailAccountId,
                gmailThreadId
        );
    }
}
