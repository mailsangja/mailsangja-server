package com.mailsangja.db.port;

import com.mailsangja.db.dto.MessageLabelView;
import com.mailsangja.db.dto.ThreadMessageLabelView;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepositoryPort {
    Message save(Message message);
    Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId);
    Optional<Message> findByThreadIdAndGmailMessageId(UUID threadId, String gmailMessageId);
    Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
            UUID mailAccountId,
            String gmailThreadId,
            String gmailMessageId
    );
    Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
            UUID mailAccountId,
            String gmailThreadId,
            String gmailMessageId
    );
    boolean existsByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndGmailMessageIdNot(
            UUID mailAccountId,
            String gmailThreadId,
            String gmailMessageId
    );
    boolean existsByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
            UUID mailAccountId,
            String gmailThreadId,
            Direction direction
    );
    Optional<Message> findByIdIncludingDeleted(UUID messageId);
    Optional<Message> findByIdIncludingDeletedAndSensitiveLabelsExcluded(UUID messageId);
    List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId);
    List<Message> findAllByThreadIdIncludingDeleted(UUID threadId);
    List<Message> findAllByThreadIdInAndDeletedAtIsNull(List<UUID> threadIds);
    List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId);
    List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndSensitiveLabelsExcluded(UUID mailAccountId, String gmailThreadId);
    List<Message> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    Slice<Message> findDeletedByUserId(UUID userId, UUID markerId, Pageable pageable);
    Slice<Message> findDeletedByUserIdAndFilters(UUID userId, UUID markerId, List<UUID> labelIds, Boolean read, Pageable pageable);
    long countUnreadDeletedByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read);
    long countDeletedByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read);
    List<Message> findAllDeletedByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    int bulkSoftDeleteByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId, LocalDateTime deletedAt);
    int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    void hardDelete(Message message);
    boolean existsByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);

    List<Message> findAllByUserIdAndDeletedAtIsNullWithLabels(UUID userId);

    List<Message> findAllByUserIdAndDeletedAtIsNullWithAttachments(UUID userId);

    List<Message> saveAll(List<Message> messages);

    List<ThreadMessageLabelView> findLabelsByThreadIdIn(List<UUID> threadIds);

    int deleteMessageLabelsByLabelId(UUID labelId);

    Optional<Message> findByIdWithLabelsAndDeletedAtIsNull(UUID id);

    List<Message> findActiveMessagesWithLabelsByUserIdPaged(UUID userId, org.springframework.data.domain.Pageable pageable);

    List<MessageLabelView> findMessageLabelsByMessageIds(List<UUID> messageIds);

    List<UUID> findActiveThreadIdsByUserId(UUID userId);

    List<Message> findActiveMessagesWithLabelsByThreadIdIn(List<UUID> threadIds);

    List<Message> findRecentByUserIdAndDirection(UUID userId, Direction direction, Pageable pageable);
}
