package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GmailNewMessageApplyCommandService {

    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;
    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final InitialMailSyncCommandService initialMailSyncCommandService;

    @Transactional
    public UUID applyNewMessageSync(
            MailAccount mailAccount,
            GmailHistoryEvent event,
            InitialMailSyncThreadSaveCommand syncCommand
    ) {
        gmailThreadLockRepositoryPort.acquireThreadLock(mailAccount, event.gmailThreadId());

        restoreIfDeleted(mailAccount, event.gmailThreadId());

        initialMailSyncCommandService.saveThreadBatch(mailAccount, List.of(syncCommand));

        return findMessageIdByGmailMessageId(event.gmailMessageId());
    }

    private UUID findMessageIdByGmailMessageId(String gmailMessageId) {
        return messageRepositoryPort.findByGmailMessageIdAndDeletedAtIsNull(gmailMessageId)
                .map(Message::getId)
                .orElse(null);
    }

    private void restoreIfDeleted(MailAccount mailAccount, String gmailThreadId) {
        // 삭제 여부를 먼저 확인하여 불필요한 bulk UPDATE 및 JPA 캐시 초기화를 방지한다.
        List<Thread> allThreads = threadRepositoryPort.findAllByMailAccountIdAndGmailThreadId(
                mailAccount.getId(), gmailThreadId
        );
        boolean anyDeleted = allThreads.stream().anyMatch(Thread::isDeleted);
        if (!anyDeleted) {
            return;
        }

        // Thread만 복구하고 messageCount를 0으로 초기화한다.
        // 기존 soft-deleted 메시지는 복구하지 않으며, 이후 saveThreadBatch에서 신규 메시지만 삽입된다.
        threadRepositoryPort.bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(
                mailAccount.getId(), gmailThreadId
        );
    }
}
