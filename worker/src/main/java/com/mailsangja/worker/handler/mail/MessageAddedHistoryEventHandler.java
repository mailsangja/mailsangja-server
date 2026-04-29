package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.service.mail.GmailNewMessageSyncCommandService;
import com.mailsangja.worker.service.notification.FcmPushCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageAddedHistoryEventHandler {

    private final GmailNewMessageSyncCommandService gmailNewMessageSyncCommandService;
    private final FcmPushCommandService fcmPushCommandService;

    public void handle(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event).ifPresent(context -> {
            try {
                fcmPushCommandService.sendNewMailPush(context);
            } catch (Exception e) {
                log.warn("FCM push skipped due to unexpected error: mailAccountId={} error={}", context.mailAccountId(), e.getMessage());
            }
        });
    }
}
