package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.module.mail.ThreadJpaRepositoryModule;
import com.mailsangja.db.port.ThreadRepositoryPort;
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
public class ThreadRepositoryAdapter implements ThreadRepositoryPort {

    private final ThreadJpaRepositoryModule threadJpaRepositoryModule;

    @Override
    public Thread save(Thread thread) {
        return threadJpaRepositoryModule.save(thread);
    }

    @Override
    public Optional<Thread> findByIdAndDeletedAtIsNull(UUID id) {
        return threadJpaRepositoryModule.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Thread> findByIdIncludingDeleted(UUID id) {
        return threadJpaRepositoryModule.findById(id);
    }

    @Override
    public Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
            UUID mailAccountId, String gmailThreadId, Direction direction
    ) {
        return threadJpaRepositoryModule.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccountId, gmailThreadId, direction
        );
    }

    @Override
    public List<Thread> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
        return threadJpaRepositoryModule.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId);
    }

    @Override
    public Slice<Thread> findInboxByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable) {
        return threadJpaRepositoryModule.findInboxByUserIdAndDeletedAtIsNull(userId, markerId, pageable);
    }

    @Override
    public Slice<Thread> findSentByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable) {
        return threadJpaRepositoryModule.findSentByUserIdAndDeletedAtIsNull(userId, markerId, pageable);
    }

    @Override
    public long countUnreadInboxByUserId(UUID userId) {
        return threadJpaRepositoryModule.countUnreadInboxByUserId(userId);
    }

    @Override
    public Slice<Thread> findTrashByUserId(UUID userId, UUID markerId, Pageable pageable) {
        return threadJpaRepositoryModule.findTrashByUserId(userId, markerId, pageable);
    }

    @Override
    public List<Thread> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
        return threadJpaRepositoryModule.findAllByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }

    @Override
    public int bulkSoftDeleteByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId, LocalDateTime deletedAt) {
        return threadJpaRepositoryModule.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, deletedAt);
    }

    @Override
    public int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
        return threadJpaRepositoryModule.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
    }
}
