package com.mailsangja.worker.service.trash;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GmailHistoryDeleteCommandService {

    private final MailAccountQueryService mailAccountQueryService;
    private final GmailHistoryDeleteApplyCommandService gmailHistoryDeleteApplyCommandService;

    public void trashMessage(GmailHistoryEvent event) {
        validateEvent(event);
        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());
        gmailHistoryDeleteApplyCommandService.applyMessageTrashed(mailAccount, event);
    }

    public void restoreMessage(GmailHistoryEvent event) {
        validateEvent(event);
        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());
        gmailHistoryDeleteApplyCommandService.applyMessageRestored(mailAccount, event);
    }

    public void permanentlyDeleteMessage(GmailHistoryEvent event) {
        validateEvent(event);
        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());
        gmailHistoryDeleteApplyCommandService.applyMessagePermanentlyDeleted(mailAccount, event);
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
