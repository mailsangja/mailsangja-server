package com.mailsangja.core.service.trash;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrashCommandService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @Transactional
    public void softDeleteThread(Thread thread) {
        gmailThreadLockRepositoryPort.acquireThreadLock(thread.getMailAccount(), thread.getGmailThreadId());

        LocalDateTime now = LocalDateTime.now();
        UUID mailAccountId = thread.getMailAccount().getId();
        String gmailThreadId = thread.getGmailThreadId();

        threadRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, now);
        messageRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, now);
    }

    @Transactional
    public void softDeleteMessage(Message message) {
        UUID mailAccountId = message.getThread().getMailAccount().getId();
        String gmailThreadId = message.getThread().getGmailThreadId();
        gmailThreadLockRepositoryPort.acquireThreadLock(message.getThread().getMailAccount(), gmailThreadId);

        message.delete();
        messageRepositoryPort.save(message);

        // 스레드 내 활성 메시지가 모두 삭제되었으면 Thread도 soft-delete
        List<Message> activeMessages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId);
        if (activeMessages.isEmpty()) {
            threadRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, LocalDateTime.now());
        }
    }

    @Transactional
    public void restoreThread(Thread thread) {
        gmailThreadLockRepositoryPort.acquireThreadLock(thread.getMailAccount(), thread.getGmailThreadId());

        UUID mailAccountId = thread.getMailAccount().getId();
        String gmailThreadId = thread.getGmailThreadId();

        threadRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
        messageRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }

    @Transactional
    public void restoreMessage(Message message) {
        UUID mailAccountId = message.getThread().getMailAccount().getId();
        String gmailThreadId = message.getThread().getGmailThreadId();
        gmailThreadLockRepositoryPort.acquireThreadLock(message.getThread().getMailAccount(), gmailThreadId);

        message.restore();
        messageRepositoryPort.save(message);
        threadRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }
}
