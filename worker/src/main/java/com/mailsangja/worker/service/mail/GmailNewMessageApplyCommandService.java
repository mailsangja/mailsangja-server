package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
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

@Service
@RequiredArgsConstructor
public class GmailNewMessageApplyCommandService {

    private final GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;
    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final InitialMailSyncCommandService initialMailSyncCommandService;

    @Transactional
    public void applyNewMessageSync(
            MailAccount mailAccount,
            GmailHistoryEvent event,
            InitialMailSyncThreadSaveCommand syncCommand
    ) {
        gmailThreadLockRepositoryPort.acquireThreadLock(mailAccount, event.gmailThreadId());

        // 삭제된 Thread/Message가 존재하면 복구한다 (신규 메시지 수신 시 삭제 상태 해제)
        restoreIfDeleted(mailAccount, event.gmailThreadId());

        initialMailSyncCommandService.saveThreadBatch(mailAccount, List.of(syncCommand));
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

        threadRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(
                mailAccount.getId(), gmailThreadId
        );
        messageRepositoryPort.bulkRestoreByMailAccountIdAndGmailThreadId(
                mailAccount.getId(), gmailThreadId
        );
    }
}
