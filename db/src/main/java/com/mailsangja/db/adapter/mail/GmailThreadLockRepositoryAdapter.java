package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.GmailThreadLock;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.module.mail.GmailThreadLockJpaRepositoryModule;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GmailThreadLockRepositoryAdapter implements GmailThreadLockRepositoryPort {

    private static final String GMAIL_THREAD_LOCK_UNIQUE_CONSTRAINT = "uq_gmail_thread_locks_account_thread";
    private static final String POSTGRES_UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final GmailThreadLockJpaRepositoryModule gmailThreadLockJpaRepositoryModule;

    @Override
    public GmailThreadLock save(GmailThreadLock gmailThreadLock) {
        return gmailThreadLockJpaRepositoryModule.saveAndFlush(gmailThreadLock);
    }

    @Override
    public void acquireThreadLock(MailAccount mailAccount, String gmailThreadId) {
        findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(
                mailAccount.getId(),
                gmailThreadId
        ).orElseGet(() -> createAndLockThreadLock(mailAccount, gmailThreadId));
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

    private GmailThreadLock createAndLockThreadLock(MailAccount mailAccount, String gmailThreadId) {
        try {
            save(GmailThreadLock.builder()
                    .mailAccount(mailAccount)
                    .gmailThreadId(gmailThreadId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            if (!isConcurrentLockCreation(e)) {
                throw e;
            }
        }

        return findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(
                        mailAccount.getId(),
                        gmailThreadId
                )
                .orElseThrow(IllegalStateException::new);
    }

    private boolean isConcurrentLockCreation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException
                    && GMAIL_THREAD_LOCK_UNIQUE_CONSTRAINT.equals(constraintViolationException.getConstraintName())) {
                return true;
            }

            if (cause instanceof SQLException sqlException
                    && POSTGRES_UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
