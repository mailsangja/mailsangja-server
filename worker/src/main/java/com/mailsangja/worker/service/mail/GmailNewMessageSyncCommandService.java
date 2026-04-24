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
import com.mailsangja.worker.service.google.GmailMessageApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailNewMessageSyncCommandService {

    private final GmailMessageApiService gmailMessageApiService;
    private final GmailNewMessageApplyCommandService gmailNewMessageApplyCommandService;

    public NewMailPushContext syncNewMessage(MailAccount mailAccount, GmailHistoryEvent event) {
        List<InitialMailSyncThreadResult> threadResults = gmailMessageApiService.getThreads(
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
}
