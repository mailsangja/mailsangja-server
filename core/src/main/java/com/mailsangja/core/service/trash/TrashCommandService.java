package com.mailsangja.core.service.trash;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrashCommandService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @Transactional
    public void softDeleteThread(Thread thread) {
        gmailThreadLockRepositoryPort.acquireThreadLock(thread.getMailAccount(), thread.getGmailThreadId());

        LocalDateTime now = LocalDateTime.now();
        UUID mailAccountId = thread.getMailAccount().getId();
        String gmailThreadId = thread.getGmailThreadId();

        threadRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, now);
        messageRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, now);
    }

    @Transactional
    public void softDeleteMessage(Message message) {
        UUID mailAccountId = message.getThread().getMailAccount().getId();
        String gmailThreadId = message.getThread().getGmailThreadId();
        gmailThreadLockRepositoryPort.acquireThreadLock(message.getThread().getMailAccount(), gmailThreadId);

        message.delete();
        messageRepositoryPort.save(message);

        List<Message> activeMessages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId);
        if (activeMessages.isEmpty()) {
            threadRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId, LocalDateTime.now());
        } else {
            updateThreadLatestInfo(mailAccountId, gmailThreadId, activeMessages);
        }
    }

    @Transactional
    public void restoreThread(Thread thread) {
        gmailThreadLockRepositoryPort.acquireThreadLock(thread.getMailAccount(), thread.getGmailThreadId());

        UUID mailAccountId = thread.getMailAccount().getId();
        String gmailThreadId = thread.getGmailThreadId();

        threadRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);
        messageRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);

        List<Message> activeMessages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId);
        if (!activeMessages.isEmpty()) {
            updateThreadLatestInfo(mailAccountId, gmailThreadId, activeMessages);
        }
    }

    @Transactional
    public void restoreMessage(Message message) {
        UUID mailAccountId = message.getThread().getMailAccount().getId();
        String gmailThreadId = message.getThread().getGmailThreadId();
        gmailThreadLockRepositoryPort.acquireThreadLock(message.getThread().getMailAccount(), gmailThreadId);

        message.restore();
        messageRepositoryPort.save(message);
        threadRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(mailAccountId, gmailThreadId);

        List<Message> activeMessages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId);
        if (!activeMessages.isEmpty()) {
            updateThreadLatestInfo(mailAccountId, gmailThreadId, activeMessages);
        }
    }

    private void updateThreadLatestInfo(UUID mailAccountId, String gmailThreadId, List<Message> activeMessages) {
        Message latest = activeMessages.stream()
                .filter(m -> m.getSentAt() != null)
                .max(Comparator.comparing(Message::getSentAt))
                .orElse(null);

        List<Thread> threads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccountId, gmailThreadId
        );

        boolean allRead = activeMessages.stream().allMatch(Message::isRead);
        int activeCount = activeMessages.size();

        for (Thread thread : threads) {
            if (latest != null) {
                thread.updateLatestMessageInfo(
                        latest.getSubject(),
                        latest.getSnippet(),
                        resolveParticipantAddress(thread.getDirection(), latest),
                        resolveParticipantName(thread.getDirection(), latest),
                        latest.getSentAt()
                );
            }
            thread.updateReadStatus(allRead);
            thread.updateMessageCount(activeCount);
        }
    }

    private String resolveParticipantAddress(Direction direction, Message message) {
        if (direction == Direction.OUTBOUND) {
            List<String> toAddresses = message.getToAddresses();
            if (toAddresses != null && !toAddresses.isEmpty()) {
                return toAddresses.getFirst();
            }
            List<String> ccAddresses = message.getCcAddresses();
            return (ccAddresses == null || ccAddresses.isEmpty()) ? null : ccAddresses.getFirst();
        }
        return message.getFromAddress();
    }

    private String resolveParticipantName(Direction direction, Message message) {
        if (direction == Direction.OUTBOUND) {
            List<String> toNames = message.getToNames();
            if (toNames != null && !toNames.isEmpty()) {
                return normalizeName(toNames.getFirst(), resolveParticipantAddress(direction, message));
            }
            List<String> ccNames = message.getCcNames();
            if (ccNames != null && !ccNames.isEmpty()) {
                return normalizeName(ccNames.getFirst(), resolveParticipantAddress(direction, message));
            }
            return resolveParticipantAddress(direction, message);
        }
        return normalizeName(message.getFromName(), message.getFromAddress());
    }

    private String normalizeName(String name, String fallbackAddress) {
        if (name == null) {
            return fallbackAddress;
        }
        String trimmed = name.trim();
        return trimmed.isBlank() ? fallbackAddress : trimmed;
    }
}
