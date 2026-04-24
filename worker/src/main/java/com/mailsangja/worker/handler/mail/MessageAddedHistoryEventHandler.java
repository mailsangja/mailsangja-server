package com.mailsangja.worker.handler.mail;

import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import com.mailsangja.worker.service.mail.GmailNewMessageSyncCommandService;
import com.mailsangja.worker.service.notification.FcmPushCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageAddedHistoryEventHandler implements GmailHistoryEventHandler {

    private final GmailNewMessageSyncCommandService gmailNewMessageSyncCommandService;
    private final FcmPushCommandService fcmPushCommandService;

    @Override
    public GmailHistoryEventType supports() {
        return GmailHistoryEventType.MESSAGE_ADDED;
    }

    @Override
    public void handle(GmailHistoryEvent event) {
        NewMailPushContext context = gmailNewMessageSyncCommandService.syncNewMessage(event);
        try {
            fcmPushCommandService.sendNewMailPush(context);
        } catch (Exception e) {
            log.warn("FCM push skipped due to unexpected error: mailAccountId={} error={}", context.mailAccountId(), e.getMessage());
        }
    }
}
