package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.module.mail.ThreadJpaRepositoryModule;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

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
    public Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
            UUID mailAccountId, String gmailThreadId, Direction direction
    ) {
        return threadJpaRepositoryModule.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccountId, gmailThreadId, direction
        );
    }

    @Override
    public Slice<Thread> findInboxByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable) {
        return threadJpaRepositoryModule.findInboxByUserIdAndDeletedAtIsNull(userId, markerId, pageable);
    }

    @Override
    public Slice<Thread> findSentByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable) {
        return threadJpaRepositoryModule.findSentByUserIdAndDeletedAtIsNull(userId, markerId, pageable);
    }
}
