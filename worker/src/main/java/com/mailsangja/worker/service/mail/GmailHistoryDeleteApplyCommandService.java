package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GmailHistoryDeleteApplyCommandService {

    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;
    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    @Transactional
    public void applyThreadTrashed(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailThreadLockRepositoryPort.acquireThreadLock(mailAccount, event.gmailThreadId());

        LocalDateTime deletedAt = LocalDateTime.now();
        threadRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccount.getId(), event.gmailThreadId(), deletedAt);
        messageRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccount.getId(), event.gmailThreadId(), deletedAt);
    }

    @Transactional
    public void applyThreadRestored(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailThreadLockRepositoryPort.acquireThreadLock(mailAccount, event.gmailThreadId());

        threadRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccount.getId(), event.gmailThreadId());
        messageRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccount.getId(), event.gmailThreadId());
    }
}
