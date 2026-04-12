package com.mailsangja.core.service.trash;

import com.mailsangja.core.common.exception.trash.TrashErrorCode;
import com.mailsangja.core.common.exception.trash.TrashException;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrashQueryService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    public List<Thread> findTrashThreadsByUserId(UUID userId) {
        return threadRepositoryPort.findTrashByUserId(userId);
    }

    public Thread findActiveThreadById(UUID threadId) {
        return threadRepositoryPort.findByIdAndDeletedAtIsNull(threadId)
                .orElseThrow(() -> new TrashException(TrashErrorCode.THREAD_NOT_FOUND));
    }

    public Thread findDeletedThreadById(UUID threadId) {
        Thread thread = threadRepositoryPort.findByIdIncludingDeleted(threadId)
                .orElseThrow(() -> new TrashException(TrashErrorCode.THREAD_NOT_FOUND));
        if (!thread.isDeleted()) {
            throw new TrashException(TrashErrorCode.THREAD_NOT_DELETED);
        }
        return thread;
    }

    public Message findActiveMessageById(UUID messageId) {
        Message message = messageRepositoryPort.findByIdIncludingDeleted(messageId)
                .orElseThrow(() -> new TrashException(TrashErrorCode.MESSAGE_NOT_FOUND));
        if (message.isDeleted()) {
            throw new TrashException(TrashErrorCode.MESSAGE_NOT_FOUND);
        }
        return message;
    }

    public Message findDeletedMessageById(UUID messageId) {
        Message message = messageRepositoryPort.findByIdIncludingDeleted(messageId)
                .orElseThrow(() -> new TrashException(TrashErrorCode.MESSAGE_NOT_FOUND));
        if (!message.isDeleted()) {
            throw new TrashException(TrashErrorCode.MESSAGE_NOT_DELETED);
        }
        return message;
    }
}
