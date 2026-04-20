package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageSaveCommand;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import com.mailsangja.worker.dto.mail.sync.NewMessageApplyResult;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import com.mailsangja.worker.service.google.GoogleMailMessageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailNewMessageSyncCommandService {

    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleMailMessageQueryService googleMailMessageQueryService;
    private final GmailNewMessageApplyCommandService gmailNewMessageApplyCommandService;

    public NewMailPushContext syncNewMessage(GmailHistoryEvent event) {
        validateEvent(event);

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());

        List<InitialMailSyncThreadResult> threadResults = googleMailMessageQueryService.getThreads(
                mailAccount.getAccessToken(),
                List.of(event.gmailThreadId())
        );

        if (threadResults.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        InitialMailSyncThreadSaveCommand syncCommand = InitialMailSyncThreadSaveCommand.from(threadResults.getFirst());
        NewMessageApplyResult applyResult = gmailNewMessageApplyCommandService.applyNewMessageSync(mailAccount, event, syncCommand);

        String subject = null;
        String snippet = null;
        for (InitialMailSyncMessageSaveCommand message : syncCommand.messages()) {
            if (event.gmailMessageId().equals(message.gmailMessageId())) {
                subject = message.subject();
                snippet = message.snippet();
                break;
            }
        }

        return new NewMailPushContext(
                mailAccount.getId(),
                mailAccount.getAlias(),
                subject,
                snippet,
                applyResult.threadId(),
                applyResult.messageId()
        );
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
