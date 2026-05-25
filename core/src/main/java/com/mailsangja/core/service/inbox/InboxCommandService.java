package com.mailsangja.core.service.inbox;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InboxCommandService {

    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final ThreadRepositoryPort threadRepositoryPort;

    @Transactional
    public void markThreadAsRead(Thread thread) {
        gmailThreadLockRepositoryPort.acquireThreadLock(thread.getMailAccount(), thread.getGmailThreadId());

        List<Thread> threads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                thread.getMailAccount().getId(),
                thread.getGmailThreadId()
        );
        threads.forEach(targetThread -> targetThread.updateReadStatus(true));

        List<Message> messages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                thread.getMailAccount().getId(),
                thread.getGmailThreadId()
        );
        messages.forEach(Message::markAsRead);
    }

    @Transactional
    public void markMessageAsRead(Message message) {
        gmailThreadLockRepositoryPort.acquireThreadLock(message.getThread().getMailAccount(), message.getThread().getGmailThreadId());

        message.markAsRead();

        List<Message> messages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                message.getThread().getMailAccount().getId(),
                message.getThread().getGmailThreadId()
        );
        boolean allMessagesRead = messages.stream().allMatch(Message::isRead);

        List<Thread> threads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                message.getThread().getMailAccount().getId(),
                message.getThread().getGmailThreadId()
        );
        threads.forEach(thread -> thread.updateReadStatus(allMessagesRead));
    }

    @Transactional
    public void markMessageAsUnread(Message message) {
        gmailThreadLockRepositoryPort.acquireThreadLock(message.getThread().getMailAccount(), message.getThread().getGmailThreadId());

        message.markAsUnread();

        List<Thread> threads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                message.getThread().getMailAccount().getId(),
                message.getThread().getGmailThreadId()
        );
        threads.forEach(thread -> thread.updateReadStatus(false));
    }

    @Transactional
    public void markThreadAsUnread(Thread thread) {
        gmailThreadLockRepositoryPort.acquireThreadLock(thread.getMailAccount(), thread.getGmailThreadId());

        List<Thread> threads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                thread.getMailAccount().getId(),
                thread.getGmailThreadId()
        );
        threads.forEach(targetThread -> targetThread.updateReadStatus(false));

        List<Message> messages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                thread.getMailAccount().getId(),
                thread.getGmailThreadId()
        );
        messages.forEach(Message::markAsUnread);
    }

    @Transactional
    public boolean toggleStar(Thread thread) {
        gmailThreadLockRepositoryPort.acquireThreadLock(thread.getMailAccount(), thread.getGmailThreadId());
        thread.toggleStar();
        return thread.isStar();
    }
}
