package com.mailsangja.db.port;

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
    Optional<Message> findByIdIncludingDeleted(UUID messageId);
    List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId);
    List<Message> findAllByThreadIdIncludingDeleted(UUID threadId);
    List<Message> findAllByThreadIdInAndDeletedAtIsNull(List<UUID> threadIds);
    List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId);
    List<Message> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    Slice<Message> findDeletedByUserId(UUID userId, UUID markerId, Pageable pageable);
    List<Message> findAllDeletedByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    int bulkSoftDeleteByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId, LocalDateTime deletedAt);
    int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    void hardDelete(Message message);
    boolean existsByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
}
