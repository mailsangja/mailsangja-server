package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import com.mailsangja.worker.service.mail.GmailNewMessageSyncCommandService;
import com.mailsangja.worker.service.notification.FcmPushCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageAddedHistoryEventHandler {

    private final GmailNewMessageSyncCommandService gmailNewMessageSyncCommandService;
    private final FcmPushCommandService fcmPushCommandService;

    public void handle(MailAccount mailAccount, GmailHistoryEvent event) {
        NewMailPushContext context = gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event);
        fcmPushCommandService.sendNewMailPush(context);
    }
}
