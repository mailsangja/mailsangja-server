package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Thread;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;
import java.util.UUID;

public interface ThreadRepositoryPort {
    Thread save(Thread thread);
    Optional<Thread> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Thread> findByIdIncludingDeleted(UUID id);
    Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId, Direction direction);
    Slice<Thread> findInboxByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable);
    Slice<Thread> findSentByUserIdAndDeletedAtIsNull(UUID userId, UUID markerId, Pageable pageable);
    long countUnreadInboxByUserId(UUID userId);
    Slice<Thread> findTrashByUserId(UUID userId, UUID markerId, Pageable pageable);
}
