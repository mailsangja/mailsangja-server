package com.mailsangja.core.service.inbox;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InboxCommandService {

    private final MessageRepositoryPort messageRepositoryPort;
    private final ThreadRepositoryPort threadRepositoryPort;

    @Transactional
    public void markThreadAsRead(Thread thread) {
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
    public void markThreadAsUnread(Thread thread) {
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
}
