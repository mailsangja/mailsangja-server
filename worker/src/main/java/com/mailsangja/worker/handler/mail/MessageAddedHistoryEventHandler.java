package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.common.label.NotificationPolicy;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.label.MessageBatch;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import com.mailsangja.worker.handler.label.LabelRuleCompiler;
import com.mailsangja.worker.service.label.LabelQueryService;
import com.mailsangja.worker.service.label.MessageLabelCommandService;
import com.mailsangja.worker.service.mail.GmailNewMessageSyncCommandService;
import com.mailsangja.worker.service.notification.FcmPushCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageAddedHistoryEventHandler {

    private final GmailNewMessageSyncCommandService gmailNewMessageSyncCommandService;
    private final LabelQueryService labelQueryService;
    private final LabelRuleCompiler labelRuleCompiler;
    private final MessageLabelCommandService messageLabelCommandService;
    private final AttachmentRepositoryPort attachmentRepositoryPort;
    private final FcmPushCommandService fcmPushCommandService;

    public void handle(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event).ifPresent(context -> {
            List<Label> activeLabels = labelQueryService.findAllActiveByUserId(mailAccount.getUser().getId());

            boolean isOutbound = context.direction() == Direction.OUTBOUND;

            if (activeLabels.isEmpty()) {
                if (!isOutbound) {
                    sendFcmPush(context);
                }
                return;
            }

            boolean notificationShouldSend = applyLabelsAndCheckNotification(context, activeLabels);

            if (!isOutbound && notificationShouldSend) {
                sendFcmPush(context);
            }
        });
    }

    private boolean applyLabelsAndCheckNotification(NewMailPushContext context, List<Label> activeLabels) {
        try {
            Message message = labelQueryService.findActiveMessageWithLabelsById(context.messageId())
                    .orElse(null);

            if (message == null) {
                log.warn("Message not found for labeling: messageId={}", context.messageId());
                return true;
            }

            Set<UUID> messageIdsWithAttachments = Set.of();
            boolean hasAttachment = !attachmentRepositoryPort.findAllByMessageIdAndDeletedAtIsNull(context.messageId()).isEmpty();
            if (hasAttachment) {
                messageIdsWithAttachments = Set.of(context.messageId());
            }

            Set<UUID> activeLabelIds = new java.util.HashSet<>();
            for (Label label : activeLabels) {
                activeLabelIds.add(label.getId());
            }

            MessageBatch batch = new MessageBatch(List.of(message), messageIdsWithAttachments);
            Map<UUID, List<Label>> labelsByMessageId = labelRuleCompiler.compile(activeLabels, batch);
            messageLabelCommandService.applyLabels(List.of(message), labelsByMessageId, activeLabelIds);

            List<Label> matchedLabels = labelsByMessageId.getOrDefault(context.messageId(), List.of());
            return resolveNotification(matchedLabels);

        } catch (Exception e) {
            log.warn("Label apply failed for messageId={} — falling back to send FCM push. error={}",
                    context.messageId(), e.getMessage());
            return true;
        }
    }

    private boolean resolveNotification(List<Label> matchedLabels) {
        if (matchedLabels.stream().anyMatch(l -> l.getNotificationPolicy() == NotificationPolicy.URGENT)) {
            return true;
        }
        if (matchedLabels.stream().anyMatch(l -> l.getNotificationPolicy() == NotificationPolicy.SILENT)) {
            return false;
        }
        return true;
    }

    private void sendFcmPush(NewMailPushContext context) {
        try {
            fcmPushCommandService.sendNewMailPush(context);
        } catch (Exception e) {
            log.warn("FCM push skipped due to unexpected error: mailAccountId={} error={}", context.mailAccountId(), e.getMessage());
        }
    }
}
