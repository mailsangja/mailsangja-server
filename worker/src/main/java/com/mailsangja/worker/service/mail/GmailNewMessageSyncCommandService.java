package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.GoogleMailApiContext;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GmailNewMessageSyncCommandService {

    private final GmailMessageApiService gmailMessageApiService;
    private final GmailNewMessageApplyCommandService gmailNewMessageApplyCommandService;
    private final MessageRepositoryPort messageRepositoryPort;

    public Optional<NewMailPushContext> syncNewMessage(MailAccount mailAccount, GmailHistoryEvent event) {
        boolean isNewMessage = messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccount.getId(), event.gmailThreadId(), event.gmailMessageId()
        ).isEmpty();

        List<InitialMailSyncThreadResult> threadResults = gmailMessageApiService.getThreads(
                GoogleMailApiContext.from(mailAccount),
                List.of(event.gmailThreadId())
        );

        if (threadResults.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        InitialMailSyncThreadSaveCommand syncCommand = InitialMailSyncThreadSaveCommand.from(threadResults.getFirst());
        int threadMessageCount = gmailNewMessageApplyCommandService.applyNewMessageSync(mailAccount, event, syncCommand);

        if (!isNewMessage) {
            return Optional.empty();
        }

        NewMessageApplyResult applyResult = gmailNewMessageApplyCommandService.findNewMessageApplyResult(
                mailAccount.getId(), event.gmailThreadId(), event.gmailMessageId(), threadMessageCount
        );

        for (InitialMailSyncMessageSaveCommand message : syncCommand.messages()) {
            if (event.gmailMessageId().equals(message.gmailMessageId())) {
                return Optional.of(new NewMailPushContext(
                        mailAccount.getId(),
                        mailAccount.getAlias(),
                        message.subject(),
                        message.snippet(),
                        applyResult.threadId(),
                        applyResult.messageId(),
                        message.direction(),
                        message.toAddresses(),
                        applyResult.threadMessageCount()
                ));
            }
        }

        return Optional.empty();
    }
}
