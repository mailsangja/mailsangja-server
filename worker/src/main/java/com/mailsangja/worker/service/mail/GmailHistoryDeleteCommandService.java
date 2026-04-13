package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GmailHistoryDeleteCommandService {

    private final MailAccountQueryService mailAccountQueryService;
    private final GmailHistoryDeleteApplyCommandService gmailHistoryDeleteApplyCommandService;

    public void trashThread(GmailHistoryEvent event) {
        validateEvent(event);
        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());
        gmailHistoryDeleteApplyCommandService.applyThreadTrashed(mailAccount, event);
    }

    public void restoreThread(GmailHistoryEvent event) {
        validateEvent(event);
        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());
        gmailHistoryDeleteApplyCommandService.applyThreadRestored(mailAccount, event);
    }

    private void validateEvent(GmailHistoryEvent event) {
        if (event == null
                || event.mailAccountId() == null
                || isBlank(event.gmailThreadId())
                || isBlank(event.gmailMessageId())) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
