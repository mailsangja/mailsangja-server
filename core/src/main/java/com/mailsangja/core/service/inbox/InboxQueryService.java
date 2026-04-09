package com.mailsangja.core.service.inbox;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.inbox.ThreadDetailResult;
import com.mailsangja.core.dto.inbox.ThreadListResult;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.ContactRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class InboxQueryService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final ContactRepositoryPort contactRepositoryPort;

    public Thread findThreadById(UUID threadId) {
        return threadRepositoryPort.findByIdAndDeletedAtIsNull(threadId)
                .orElseThrow(() -> new InboxException(InboxErrorCode.THREAD_NOT_FOUND));
    }

    public ThreadListResult findInboxThreadsResult(UUID userId, UUID markerId, Pageable pageable) {
        Slice<Thread> threads = threadRepositoryPort.findInboxByUserIdAndDeletedAtIsNull(userId, markerId, pageable);
        return buildThreadListResult(threads);
    }

    public ThreadListResult findSentThreadsResult(UUID userId, UUID markerId, Pageable pageable) {
        Slice<Thread> threads = threadRepositoryPort.findSentByUserIdAndDeletedAtIsNull(userId, markerId, pageable);
        return buildThreadListResult(threads);
    }

    public ThreadDetailResult findThreadDetailResult(Thread thread) {
        List<Message> messages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                thread.getMailAccount().getId(), thread.getGmailThreadId());
        Map<String, String> contactNameByEmail = findContactNamesByEmails(collectEmailsFromMessages(messages));
        return new ThreadDetailResult(thread, messages, contactNameByEmail);
    }

    private ThreadListResult buildThreadListResult(Slice<Thread> threads) {
        List<UUID> threadIds = threads.getContent().stream().map(Thread::getId).toList();
        Map<UUID, List<Attachment>> attachmentsByThreadId = findAttachmentsByThreadIds(threadIds);

        List<String> participantEmails = threads.getContent().stream()
                .map(Thread::getLatestParticipantAddress)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
        Map<String, String> contactNameByEmail = findContactNamesByEmails(participantEmails);

        return new ThreadListResult(threads, attachmentsByThreadId, contactNameByEmail);
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

    private List<String> collectEmailsFromMessages(List<Message> messages) {
        return messages.stream()
                .flatMap(m -> {
                    List<String> addrs = new ArrayList<>();
                    if (m.getFromAddress() != null) addrs.add(m.getFromAddress());
                    if (m.getToAddresses() != null) addrs.addAll(m.getToAddresses());
                    if (m.getCcAddresses() != null) addrs.addAll(m.getCcAddresses());
                    return addrs.stream();
                })
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
    }

    private Map<String, String> findContactNamesByEmails(List<String> emails) {
        if (emails.isEmpty()) {
            return Map.of();
        }
        return contactRepositoryPort.findAllByEmailInAndDeletedAtIsNull(emails)
                .stream()
                .collect(Collectors.toMap(Contact::getEmail, Contact::getName));
    }
}
