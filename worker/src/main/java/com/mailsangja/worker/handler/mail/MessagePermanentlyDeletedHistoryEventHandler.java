package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import com.mailsangja.worker.service.trash.GmailHistoryDeleteApplyCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagePermanentlyDeletedHistoryEventHandler implements GmailHistoryEventHandler {

    private final MailAccountQueryService mailAccountQueryService;
    private final GmailHistoryDeleteApplyCommandService gmailHistoryDeleteApplyCommandService;

    @Override
    public GmailHistoryEventType supports() {
        return GmailHistoryEventType.MESSAGE_PERMANENTLY_DELETED;
    }

    @Override
    public void handle(GmailHistoryEvent event) {
        MailAccount mailAccount = mailAccountQueryService.findSyncableMailAccountById(event.mailAccountId());
        gmailHistoryDeleteApplyCommandService.applyMessagePermanentlyDeleted(mailAccount, event);
    }
}
