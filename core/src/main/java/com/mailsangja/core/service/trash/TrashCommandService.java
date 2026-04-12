package com.mailsangja.core.service.trash;

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
public class TrashCommandService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    @Transactional
    public void softDeleteThread(Thread thread) {
        thread.delete();
        threadRepositoryPort.save(thread);

        List<Message> messages = messageRepositoryPort.findAllByThreadIdIncludingDeleted(thread.getId());
        for (Message message : messages) {
            if (!message.isDeleted()) {
                message.delete();
                messageRepositoryPort.save(message);
            }
        }
    }

    @Transactional
    public void softDeleteMessage(Message message) {
        message.delete();
        messageRepositoryPort.save(message);
    }

    @Transactional
    public void restoreThread(Thread thread) {
        thread.restore();
        threadRepositoryPort.save(thread);

        List<Message> messages = messageRepositoryPort.findAllByThreadIdIncludingDeleted(thread.getId());
        for (Message message : messages) {
            if (message.isDeleted()) {
                message.restore();
                messageRepositoryPort.save(message);
            }
        }
    }

    @Transactional
    public void restoreMessage(Message message) {
        message.restore();
        messageRepositoryPort.save(message);
    }
}
