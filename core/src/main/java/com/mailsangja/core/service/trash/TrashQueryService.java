package com.mailsangja.core.service.trash;

import com.mailsangja.core.common.exception.trash.TrashErrorCode;
import com.mailsangja.core.common.exception.trash.TrashException;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import com.mailsangja.db.port.ContactRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.MessageLabelView;
import com.mailsangja.db.port.ThreadLabelView;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrashQueryService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final ContactRepositoryPort contactRepositoryPort;
    private final AttachmentRepositoryPort attachmentRepositoryPort;

    public Slice<Message> findDeletedMessagesByUserId(UUID userId, UUID markerId, int size) {
        return messageRepositoryPort.findDeletedByUserId(userId, markerId, PageRequest.of(0, size));
    }

    public List<Message> findDeletedMessagesByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
        return messageRepositoryPort.findAllDeletedByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }

    public Thread findThreadByIdIncludingDeleted(UUID threadId) {
        return threadRepositoryPort.findByIdIncludingDeleted(threadId)
                .orElseThrow(() -> new TrashException(TrashErrorCode.THREAD_NOT_FOUND));
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

    public Map<UUID, List<ThreadLabelView>> findLabelsByThreadIds(List<UUID> threadIds) {
        if (threadIds.isEmpty()) {
            return Map.of();
        }
        return messageRepositoryPort.findLabelsByThreadIdIn(threadIds)
                .stream()
                .collect(Collectors.groupingBy(ThreadLabelView::threadId));
    }

    public Map<UUID, List<MessageLabelView>> findMessageLabelsByMessageIds(List<UUID> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return messageRepositoryPort.findMessageLabelsByMessageIds(messageIds)
                .stream()
                .collect(Collectors.groupingBy(MessageLabelView::messageId));
    }

    public Map<String, String> findContactNamesByEmails(List<String> emails) {
        if (emails.isEmpty()) {
            return Map.of();
        }
        return contactRepositoryPort.findAllByEmailInAndDeletedAtIsNull(emails)
                .stream()
                .collect(Collectors.toMap(Contact::getEmail, Contact::getName));
    }

    public Map<UUID, List<Attachment>> findAttachmentsByMessageIds(List<UUID> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return attachmentRepositoryPort.findAllByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getMessage().getId()));
    }
}
