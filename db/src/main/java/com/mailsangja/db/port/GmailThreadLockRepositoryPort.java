package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.GmailThreadLock;

import java.util.Optional;
import java.util.UUID;

public interface GmailThreadLockRepositoryPort {
    GmailThreadLock save(GmailThreadLock gmailThreadLock);
    Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId);
    Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(UUID mailAccountId, String gmailThreadId);
}
