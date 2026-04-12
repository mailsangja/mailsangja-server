package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.GmailThreadLock;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.GmailHistoryEvent;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadSaveCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailHistoryStateApplyCommandService {

    // INBOUND/OUTBOUND row가 분리되어 있어도 Gmail read state는 동일 thread 기준으로 집계한다.
    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;
    private final GmailHistoryStateQueryService gmailHistoryStateQueryService;
    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final InitialMailSyncCommandService initialMailSyncCommandService;

    @Transactional
    public void applyMessageReadState(
            MailAccount mailAccount,
            GmailHistoryEvent event,
            boolean read,
            InitialMailSyncThreadSaveCommand syncCommand
    ) {
        acquireThreadLock(mailAccount, event.gmailThreadId());

        if (syncCommand != null && !gmailHistoryStateQueryService.existsMessage(
                mailAccount.getId(),
                event.gmailThreadId(),
                event.gmailMessageId()
        )) {
            initialMailSyncCommandService.saveMissingMessagesFromThreadSnapshot(mailAccount, syncCommand);
        }

        List<Message> messages = messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccount.getId(),
                event.gmailThreadId()
        );
        Message targetMessage = messages.stream()
                .filter(message -> event.gmailMessageId().equals(message.getGmailMessageId()))
                .findFirst()
                .orElseThrow(() -> new MailPushException(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID));

        updateMessageReadStatus(targetMessage, read);
        updateThreadReadStatus(mailAccount, event, messages, targetMessage, read);
    }

    private void updateThreadReadStatus(
            MailAccount mailAccount,
            GmailHistoryEvent event,
            List<Message> messages,
            Message targetMessage,
            boolean read
    ) {
        boolean hasUnreadMessage = messages.stream()
                .anyMatch(message -> {
                    if (message.getId().equals(targetMessage.getId())) {
                        return !read;
                    }
                    return !message.isRead();
                });

        List<Thread> threads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccount.getId(),
                event.gmailThreadId()
        );

        if (threads.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID);
        }

        for (Thread thread : threads) {
            thread.updateReadStatus(!hasUnreadMessage);
            if (!isBlank(event.historyId())) {
                thread.updateHistoryId(event.historyId());
            }
        }
    }

    private void updateMessageReadStatus(Message message, boolean read) {
        if (read) {
            message.markAsRead();
            return;
        }

        message.markAsUnread();
    }

    private void acquireThreadLock(MailAccount mailAccount, String gmailThreadId) {
        gmailThreadLockRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(
                mailAccount.getId(),
                gmailThreadId
        ).orElseGet(() -> createAndLockThreadLock(mailAccount, gmailThreadId));
    }

    private GmailThreadLock createAndLockThreadLock(MailAccount mailAccount, String gmailThreadId) {
        try {
            gmailThreadLockRepositoryPort.save(GmailThreadLock.builder()
                    .mailAccount(mailAccount)
                    .gmailThreadId(gmailThreadId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 thread lock row를 생성한 경우를 허용하고, 그 외 제약조건 문제는 다시 실패시킨다.
            if (!isConcurrentLockCreation(e)) {
                throw e;
            }
        }

        return gmailThreadLockRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(
                        mailAccount.getId(),
                        gmailThreadId
                )
                .orElseThrow(() -> new MailPushException(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isConcurrentLockCreation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("uq_gmail_thread_locks_account_thread")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
