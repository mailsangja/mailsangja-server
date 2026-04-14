package com.mailsangja.worker.service.trash;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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

        boolean hasOtherActiveMessages = messageRepositoryPort.existsByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndGmailMessageIdNot(
                        mailAccount.getId(), event.gmailThreadId(), event.gmailMessageId()
        );

        if (!hasOtherActiveMessages) {
            threadRepositoryPort.bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                    mailAccount.getId(), event.gmailThreadId(), LocalDateTime.now()
            );
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
}
