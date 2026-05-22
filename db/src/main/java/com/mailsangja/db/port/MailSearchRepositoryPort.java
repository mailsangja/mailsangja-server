package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface MailSearchRepositoryPort {
    Slice<Thread> searchThreads(UUID userId, String query, UUID markerId, Pageable pageable);

    Slice<Thread> searchInboxThreads(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable);

    Slice<Thread> searchSentThreads(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable);

    Slice<Message> searchTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable);

    long countInboxThreads(UUID userId, String query, List<UUID> labelIds, Boolean read);

    long countUnreadInboxThreads(UUID userId, String query, List<UUID> labelIds, Boolean read);

    long countSentThreads(UUID userId, String query, List<UUID> labelIds, Boolean read);

    long countUnreadSentThreads(UUID userId, String query, List<UUID> labelIds, Boolean read);

    long countTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read);

    long countUnreadTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read);
}
