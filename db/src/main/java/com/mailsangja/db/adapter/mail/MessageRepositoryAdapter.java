package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.module.mail.MessageJpaRepositoryModule;
import com.mailsangja.db.dto.MessageLabelView;
import com.mailsangja.db.dto.ThreadLabelView;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepositoryPort {

    private final MessageJpaRepositoryModule messageJpaRepositoryModule;

    @Override
    public Message save(Message message) {
        return messageJpaRepositoryModule.save(message);
    }

    @Override
    public Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId) {
        return messageJpaRepositoryModule.findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(threadId, gmailMessageId);
    }

    @Override
    public Optional<Message> findByThreadIdAndGmailMessageId(UUID threadId, String gmailMessageId) {
        return messageJpaRepositoryModule.findByThreadIdAndGmailMessageId(threadId, gmailMessageId);
    }

    @Override
    public Optional<Message> findByIdIncludingDeleted(UUID messageId) {
        return messageJpaRepositoryModule.findById(messageId);
    }

    @Override
    public Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
            UUID mailAccountId,
            String gmailThreadId,
            String gmailMessageId
    ) {
        return messageJpaRepositoryModule.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccountId,
                gmailThreadId,
                gmailMessageId
        );
    }

    @Override
    public Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
            UUID mailAccountId,
            String gmailThreadId,
            String gmailMessageId
    ) {
        return messageJpaRepositoryModule.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                mailAccountId,
                gmailThreadId,
                gmailMessageId
        );
    }

    @Override
    public boolean existsByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndGmailMessageIdNot(
            UUID mailAccountId,
            String gmailThreadId,
            String gmailMessageId
    ) {
        return messageJpaRepositoryModule.existsByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndGmailMessageIdNot(
                mailAccountId,
                gmailThreadId,
                gmailMessageId
        );
    }

    @Override
    public List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId) {
        return messageJpaRepositoryModule.findAllByThreadIdAndDeletedAtIsNull(threadId);
    }

    @Override
    public List<Message> findAllByThreadIdIncludingDeleted(UUID threadId) {
        return messageJpaRepositoryModule.findAllByThreadIdIncludingDeleted(threadId);
    }

    @Override
    public List<Message> findAllByThreadIdInAndDeletedAtIsNull(List<UUID> threadIds) {
        return messageJpaRepositoryModule.findAllByThreadIdInAndDeletedAtIsNull(threadIds);
    }

    @Override
    public List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
        return messageJpaRepositoryModule.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId);
    }

    @Override
    public List<Message> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
        return messageJpaRepositoryModule.findAllByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }

    @Override
    public Slice<Message> findDeletedByUserId(UUID userId, UUID markerId, Pageable pageable) {
        return messageJpaRepositoryModule.findDeletedByUserId(userId, markerId, pageable);
    }

    @Override
    public Slice<Message> findDeletedByUserIdAndFilters(
            UUID userId,
            UUID markerId,
            List<UUID> labelIds,
            Boolean read,
            Pageable pageable
    ) {
        if (labelIds == null || labelIds.isEmpty()) {
            if (read == null) {
                return messageJpaRepositoryModule.findDeletedByUserId(userId, markerId, pageable);
            }
            return messageJpaRepositoryModule.findDeletedByUserIdAndReadFilter(userId, markerId, read, pageable);
        }
        return messageJpaRepositoryModule.findDeletedByUserIdAndLabelIdsAndReadFilter(userId, markerId, labelIds, read, pageable);
    }

    @Override
    public long countUnreadDeletedByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read) {
        if (Boolean.TRUE.equals(read)) {
            return 0L;
        }
        if (labelIds == null || labelIds.isEmpty()) {
            return messageJpaRepositoryModule.countUnreadDeletedByUserId(userId);
        }
        return messageJpaRepositoryModule.countUnreadDeletedByUserIdAndLabelIds(userId, labelIds);
    }

    @Override
    public List<Message> findAllDeletedByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
        return messageJpaRepositoryModule.findAllDeletedByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }

    @Override
    public int bulkSoftDeleteByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId, LocalDateTime deletedAt) {
        return messageJpaRepositoryModule.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, deletedAt);
    }

    @Override
    public int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
        return messageJpaRepositoryModule.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }

    @Override
    public void hardDelete(Message message) {
        messageJpaRepositoryModule.delete(message);
    }

    @Override
    public boolean existsByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
        return messageJpaRepositoryModule.existsByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }

    @Override
    public List<Message> findAllByUserIdAndDeletedAtIsNullWithLabels(UUID userId) {
        return messageJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNullWithLabels(userId);
    }

    @Override
    public List<Message> findAllByUserIdAndDeletedAtIsNullWithAttachments(UUID userId) {
        return messageJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNullWithAttachments(userId);
    }

    @Override
    public List<Message> saveAll(List<Message> messages) {
        return messageJpaRepositoryModule.saveAll(messages);
    }

    @Override
    public List<ThreadLabelView> findLabelsByThreadIdIn(List<UUID> threadIds) {
        if (threadIds.isEmpty()) {
            return List.of();
        }
        return messageJpaRepositoryModule.findLabelsByThreadIdIn(threadIds)
                .stream()
                .map(p -> new ThreadLabelView(p.getThreadId(), p.getLabelId(), p.getLabelName(), p.getLabelColorCode()))
                .toList();
    }

    @Override
    public int deleteMessageLabelsByLabelId(UUID labelId) {
        return messageJpaRepositoryModule.deleteMessageLabelsByLabelId(labelId);
    }

    @Override
    public Optional<Message> findByIdWithLabelsAndDeletedAtIsNull(UUID id) {
        return messageJpaRepositoryModule.findByIdWithLabelsAndDeletedAtIsNull(id);
    }

    @Override
    public List<Message> findActiveMessagesWithLabelsByUserIdPaged(UUID userId, Pageable pageable) {
        return messageJpaRepositoryModule.findActiveMessagesWithLabelsByUserIdPaged(userId, pageable);
    }

    @Override
    public List<MessageLabelView> findMessageLabelsByMessageIds(List<UUID> messageIds) {
        if (messageIds.isEmpty()) {
            return List.of();
        }
        return messageJpaRepositoryModule.findMessageLabelsByMessageIdIn(messageIds)
                .stream()
                .map(p -> new MessageLabelView(p.getMessageId(), p.getLabelId(), p.getLabelName(), p.getColorCode()))
                .toList();
    }

    @Override
    public List<UUID> findActiveThreadIdsByUserId(UUID userId) {
        return messageJpaRepositoryModule.findActiveThreadIdsByUserId(userId);
    }

    @Override
    public List<Message> findActiveMessagesWithLabelsByThreadIdIn(List<UUID> threadIds) {
        if (threadIds.isEmpty()) {
            return List.of();
        }
        return messageJpaRepositoryModule.findActiveMessagesWithLabelsByThreadIdIn(threadIds);
    }
}
