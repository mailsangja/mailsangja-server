package com.mailsangja.worker.service.trash;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GmailHistoryDeleteApplyCommandService {

    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;
    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    @Transactional
    public void applyMessageTrashed(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailThreadLockRepositoryPort.acquireThreadLock(mailAccount, event.gmailThreadId());

        Optional<Message> messageOpt = messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                        mailAccount.getId(), event.gmailThreadId(), event.gmailMessageId()
        );

        if (messageOpt.isEmpty()) {
            return;
        }

        Message message = messageOpt.get();
        message.delete();

        List<Message> activeMessages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccount.getId(), event.gmailThreadId()
        );

        if (activeMessages.isEmpty()) {
            threadRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                    mailAccount.getId(), event.gmailThreadId(), LocalDateTime.now()
            );
        } else {
            updateThreadLatestInfo(mailAccount.getId(), event.gmailThreadId(), activeMessages);
        }
    }

    @Transactional
    public void applyMessageRestored(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailThreadLockRepositoryPort.acquireThreadLock(mailAccount, event.gmailThreadId());

        Optional<Message> messageOpt = messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                        mailAccount.getId(), event.gmailThreadId(), event.gmailMessageId()
        );

        if (messageOpt.isEmpty() || !messageOpt.get().isDeleted()) {
            return;
        }

        messageOpt.get().restore();

        // 활성 메시지가 생겼으므로 Thread도 복원
        threadRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(
                mailAccount.getId(), event.gmailThreadId()
        );

        List<Message> activeMessages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccount.getId(), event.gmailThreadId()
        );

        if (!activeMessages.isEmpty()) {
            updateThreadLatestInfo(mailAccount.getId(), event.gmailThreadId(), activeMessages);
        }
    }

    @Transactional
    public void applyMessagePermanentlyDeleted(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailThreadLockRepositoryPort.acquireThreadLock(mailAccount, event.gmailThreadId());

        Optional<Message> messageOpt = messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                mailAccount.getId(), event.gmailThreadId(), event.gmailMessageId()
        );

        if (messageOpt.isEmpty()) {
            return;
        }

        messageRepositoryPort.hardDelete(messageOpt.get());

        boolean hasRemainingMessages = messageRepositoryPort.existsByMailAccountIdAndGmailThreadId(
                mailAccount.getId(), event.gmailThreadId()
        );

        if (!hasRemainingMessages) {
            threadRepositoryPort.hardDeleteAllByMailAccountIdAndGmailThreadId(
                    mailAccount.getId(), event.gmailThreadId()
            );
        }
    }

    private void updateThreadLatestInfo(UUID mailAccountId, String gmailThreadId, List<Message> activeMessages) {
        Message latest = activeMessages.stream()
                .filter(m -> m.getSentAt() != null)
                .max(Comparator.comparing(Message::getSentAt))
                .orElse(null);

        if (latest == null) {
            return;
        }

        List<Thread> threads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccountId, gmailThreadId
        );

        boolean allRead = activeMessages.stream().allMatch(Message::isRead);
        int activeCount = activeMessages.size();

        for (Thread thread : threads) {
            thread.updateLatestMessageInfo(
                    latest.getSubject(),
                    latest.getSnippet(),
                    resolveParticipantAddress(thread.getDirection(), latest),
                    resolveParticipantName(thread.getDirection(), latest),
                    latest.getSentAt()
            );
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
