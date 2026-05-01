package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.label.LabelReclassifyMessage;
import com.mailsangja.worker.messaging.publisher.LabelReclassifyPublisher;
import com.mailsangja.worker.service.mail.GmailNewMessageSyncCommandService;
import com.mailsangja.worker.service.label.LabelQueryService;
import com.mailsangja.worker.service.notification.FcmPushCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageAddedHistoryEventHandler {

    private final GmailNewMessageSyncCommandService gmailNewMessageSyncCommandService;
    private final LabelQueryService labelQueryService;
    private final LabelReclassifyPublisher labelReclassifyPublisher;
    private final FcmPushCommandService fcmPushCommandService;

    public void handle(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event).ifPresent(context -> {
            Set<UUID> activeLabelIds = labelQueryService.findActiveLabelIdsByUserId(mailAccount.getUser().getId());
            if (!activeLabelIds.isEmpty()) {
                try {
                    labelReclassifyPublisher.publish(new LabelReclassifyMessage(mailAccount.getUser().getId(), activeLabelIds));
                } catch (Exception e) {
                    log.warn("Label reclassify publish skipped: userId={} error={}", mailAccount.getUser().getId(), e.getMessage());
                }
            }

            try {
                fcmPushCommandService.sendNewMailPush(context);
            } catch (Exception e) {
                log.warn("FCM push skipped due to unexpected error: mailAccountId={} error={}", context.mailAccountId(), e.getMessage());
            }
        });
    }
}
