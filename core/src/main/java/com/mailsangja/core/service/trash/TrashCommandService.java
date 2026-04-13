package com.mailsangja.core.service.trash;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrashCommandService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    @Transactional
    public void softDeleteThread(Thread thread) {
        LocalDateTime now = LocalDateTime.now();
        String gmailThreadId = thread.getGmailThreadId();
        UUID mailAccountId = thread.getMailAccount().getId();

        threadRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, now);
        messageRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, now);
    }

    @Transactional
    public void softDeleteMessage(Message message) {
        message.delete();
        messageRepositoryPort.save(message);
    }

    @Transactional
    public void restoreThread(Thread thread) {
        String gmailThreadId = thread.getGmailThreadId();
        UUID mailAccountId = thread.getMailAccount().getId();

        threadRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
        messageRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }

    @Transactional
    public void restoreMessage(Message message) {
        message.restore();
        messageRepositoryPort.save(message);
    }
}
