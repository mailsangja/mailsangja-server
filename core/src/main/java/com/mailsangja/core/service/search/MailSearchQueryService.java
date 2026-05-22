package com.mailsangja.core.service.search;

import com.mailsangja.core.dto.inbox.ThreadListResult;
import com.mailsangja.db.dto.ThreadMessageLabelView;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.ContactRepositoryPort;
import com.mailsangja.db.port.MailSearchRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MailSearchQueryService {

    private final MailSearchRepositoryPort mailSearchRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final ContactRepositoryPort contactRepositoryPort;

    public ThreadListResult searchThreadsResult(UUID userId, String query, UUID markerId, Pageable pageable) {
        Slice<Thread> threads = mailSearchRepositoryPort.searchThreads(userId, query, markerId, pageable);
        return buildThreadListResult(userId, threads);
    }

    public ThreadListResult searchInboxThreadsResult(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable) {
        Slice<Thread> threads = mailSearchRepositoryPort.searchInboxThreads(userId, query, labelIds, read, markerId, pageable);
        return buildThreadListResult(userId, threads);
    }

    public ThreadListResult searchSentThreadsResult(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable) {
        Slice<Thread> threads = mailSearchRepositoryPort.searchSentThreads(userId, query, labelIds, read, markerId, pageable);
        return buildThreadListResult(userId, threads);
    }

    public Slice<Message> searchTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable) {
        return mailSearchRepositoryPort.searchTrashMessages(userId, query, labelIds, read, markerId, pageable);
    }

    public long countInboxThreads(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        return mailSearchRepositoryPort.countInboxThreads(userId, query, labelIds, read);
    }

    public long countUnreadInboxThreads(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        return mailSearchRepositoryPort.countUnreadInboxThreads(userId, query, labelIds, read);
    }

    public long countSentThreads(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        return mailSearchRepositoryPort.countSentThreads(userId, query, labelIds, read);
    }

    public long countUnreadSentThreads(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        return mailSearchRepositoryPort.countUnreadSentThreads(userId, query, labelIds, read);
    }

    public long countTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        return mailSearchRepositoryPort.countTrashMessages(userId, query, labelIds, read);
    }

    public long countUnreadTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        return mailSearchRepositoryPort.countUnreadTrashMessages(userId, query, labelIds, read);
    }

    private ThreadListResult buildThreadListResult(UUID userId, Slice<Thread> threads) {
        List<UUID> threadIds = threads.getContent().stream().map(Thread::getId).toList();

        Map<UUID, List<Attachment>> attachmentsByThreadId = findAttachmentsByThreadIds(threadIds);
        Map<UUID, List<ThreadMessageLabelView>> labelsByThreadId = findLabelsByThreadIds(threadIds);

        List<String> participantEmails = threads.getContent().stream()
                .map(Thread::getLatestParticipantAddress)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
        Map<String, String> contactNameByEmail = findContactNamesByEmails(userId, participantEmails);

        return new ThreadListResult(threads, attachmentsByThreadId, contactNameByEmail, labelsByThreadId);
    }

    private Map<UUID, List<Attachment>> findAttachmentsByThreadIds(List<UUID> threadIds) {
        if (threadIds.isEmpty()) {
            return Map.of();
        }
        List<Message> messages = messageRepositoryPort.findAllByThreadIdInAndDeletedAtIsNull(threadIds);
        return messages.stream()
                .filter(m -> !m.getAttachments().isEmpty())
                .flatMap(m -> m.getAttachments().stream()
                        .map(a -> Map.entry(m.getThread().getId(), a)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    private Map<UUID, List<ThreadMessageLabelView>> findLabelsByThreadIds(List<UUID> threadIds) {
        if (threadIds.isEmpty()) {
            return Map.of();
        }
        return messageRepositoryPort.findLabelsByThreadIdIn(threadIds)
                .stream()
                .collect(Collectors.groupingBy(ThreadMessageLabelView::threadId));
    }

    private Map<String, String> findContactNamesByEmails(UUID userId, List<String> emails) {
        if (emails.isEmpty()) {
            return Map.of();
        }
        return contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(userId, emails)
                .stream()
                .collect(Collectors.toMap(Contact::getEmail, Contact::getName));
    }
}
