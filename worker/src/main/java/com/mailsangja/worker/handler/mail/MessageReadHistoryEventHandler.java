package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.GoogleMailApiContext;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import com.mailsangja.worker.service.google.GmailMessageApiService;
import com.mailsangja.worker.service.mail.GmailHistoryStateApplyCommandService;
import com.mailsangja.worker.service.mail.GmailHistoryStateQueryService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageReadHistoryEventHandler implements GmailHistoryEventHandler {

    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    private final GmailHistoryStateQueryService gmailHistoryStateQueryService;
    private final GmailMessageApiService gmailMessageApiService;
    private final GmailHistoryStateApplyCommandService gmailHistoryStateApplyCommandService;

    @Override
    public GmailHistoryEventType supports() {
        return GmailHistoryEventType.MESSAGE_READ;
    }

    @Override
    public void handle(GmailHistoryEvent event) {
        MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
                mailAccountQueryService.findSyncableMailAccountById(event.mailAccountId())
        );
        InitialMailSyncThreadSaveCommand syncCommand = prepareSyncCommandIfNeeded(mailAccount, event);
        gmailHistoryStateApplyCommandService.applyMessageReadState(mailAccount, event, true, syncCommand);
    }

    private InitialMailSyncThreadSaveCommand prepareSyncCommandIfNeeded(MailAccount mailAccount, GmailHistoryEvent event) {
        if (gmailHistoryStateQueryService.existsMessage(mailAccount.getId(), event.gmailThreadId(), event.gmailMessageId())) {
            return null;
        }
        List<InitialMailSyncThreadResult> threadResults = gmailMessageApiService.getThreads(
                GoogleMailApiContext.from(mailAccount), List.of(event.gmailThreadId())
        );
        if (threadResults.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
        return InitialMailSyncThreadSaveCommand.from(threadResults.getFirst());
    }
}
