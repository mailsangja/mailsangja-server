package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Thread;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreadRepositoryPort {
    Thread save(Thread thread);
    Optional<Thread> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Thread> findByIdIncludingDeleted(UUID id);
    Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId, Direction direction);
    Slice<Thread> findInboxByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable);
    Slice<Thread> findInboxByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable);
    Slice<Thread> findSentByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable);
    Slice<Thread> findSentByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable);
    long countUnreadInboxByUserId(UUID userId);
    long countUnreadInboxByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read);
    long countInboxByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read);
    long countUnreadSentByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read);
    long countSentByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read);
    Slice<Thread> findTrashByUserId(UUID userId, UUID markerId, Pageable pageable);
    List<Thread> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId);
    List<Thread> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    int bulkSoftDeleteByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId, LocalDateTime deletedAt);
    int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    int bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
    void hardDeleteAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);

    Slice<Thread> findInboxByUserIdAndLabelIdsAndDeletedAtIsNull(UUID userId, List<UUID> labelIds, UUID markerId, Pageable pageable);
    Slice<Thread> findStarredByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable);
    Slice<Thread> findStarredByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable);
    long countStarredByUserId(UUID userId);
    long countUnreadStarredByUserId(UUID userId);
    long countStarredByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read);
    long countUnreadStarredByUserIdAndFilters(UUID userId, List<UUID> labelIds, Boolean read);
}
