package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.GmailHistoryEvent;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadResult;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadSaveCommand;
import com.mailsangja.worker.service.google.GoogleMailMessageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailHistoryStateCommandService {

    private final MailAccountQueryService mailAccountQueryService;
    private final GmailHistoryStateQueryService gmailHistoryStateQueryService;
    private final GoogleMailMessageQueryService googleMailMessageQueryService;
    private final GmailHistoryStateApplyCommandService gmailHistoryStateApplyCommandService;

    public void markMessageAsRead(GmailHistoryEvent event) {
        processMessageReadState(event, true);
    }

    public void markMessageAsUnread(GmailHistoryEvent event) {
        processMessageReadState(event, false);
    }

    private void processMessageReadState(GmailHistoryEvent event, boolean read) {
        validateEvent(event);

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());
        InitialMailSyncThreadSaveCommand syncCommand = prepareSyncCommandIfNeeded(mailAccount, event);

        gmailHistoryStateApplyCommandService.applyMessageReadState(mailAccount, event, read, syncCommand);
    }

    private InitialMailSyncThreadSaveCommand prepareSyncCommandIfNeeded(MailAccount mailAccount, GmailHistoryEvent event) {
        if (gmailHistoryStateQueryService.existsMessage(
                mailAccount.getId(),
                event.gmailThreadId(),
                event.gmailMessageId()
        )) {
            return null;
        }

        List<InitialMailSyncThreadResult> threadResults = googleMailMessageQueryService.getThreads(
                mailAccount.getAccessToken(),
                List.of(event.gmailThreadId())
        );

        if (threadResults.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        return InitialMailSyncThreadSaveCommand.from(threadResults.getFirst());
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
