package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountCommandService {

    private final MailAccountQueryService mailAccountQueryService;

    @Transactional
    public void updateSyncHistoryId(UUID mailAccountId, String syncHistoryId) {
        if (mailAccountId == null || isBlank(syncHistoryId)) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
        }

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(mailAccountId);
        mailAccount.updateSyncHistoryId(syncHistoryId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
