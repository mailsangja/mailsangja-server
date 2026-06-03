package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.common.label.NotificationPolicy;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.label.MessageBatch;
import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionMessage;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import com.mailsangja.worker.handler.label.LabelRuleCompiler;
import com.mailsangja.worker.messaging.publisher.MailEmbeddingPublisher;
import com.mailsangja.worker.messaging.publisher.ReplyDraftSuggestionPublisher;
import com.mailsangja.worker.service.label.LabelQueryService;
import com.mailsangja.worker.service.label.MessageLabelCommandService;
import com.mailsangja.worker.service.mail.GmailNewMessageSyncCommandService;
import com.mailsangja.worker.service.mail.ReplyDraftSuggestionQueryService;
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
    private final MailEmbeddingPublisher mailEmbeddingPublisher;
    private final ReplyDraftSuggestionQueryService replyDraftSuggestionQueryService;
    private final ReplyDraftSuggestionPublisher replyDraftSuggestionPublisher;

    public void handle(MailAccount mailAccount, GmailHistoryEvent event) {
        gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event).ifPresent(context -> {
            mailEmbeddingPublisher.publish(new MailEmbeddingMessage(context.messageId()));

            List<Label> activeLabels = labelQueryService.findAllActiveByUserId(mailAccount.getUser().getId());

            boolean isOutbound = context.direction() == Direction.OUTBOUND;
            LabelApplyResult labelApplyResult = LabelApplyResult.empty();

            if (!activeLabels.isEmpty()) {
                labelApplyResult = applyLabelsAndResolveResult(context, activeLabels);
            }

            if (!labelApplyResult.hasSensitiveLabel()) {
                publishReplyDraftSuggestionIfEligible(mailAccount, event, context);
            }

            if (!isOutbound && labelApplyResult.notificationShouldSend()) {
                sendFcmPush(context);
            }
        });
    }

    private void publishReplyDraftSuggestionIfEligible(MailAccount mailAccount, GmailHistoryEvent event, NewMailPushContext context) {
        if (context.direction() != Direction.INBOUND) {
            log.debug("Reply draft suggestion skipped because message is outbound. mailAccountId={} messageId={}",
                    context.mailAccountId(), context.messageId());
            return;
        }
        if (!isSentToConnectedMailAccount(mailAccount, context.toAddresses())) {
            log.debug("Reply draft suggestion skipped because message was not sent to connected account. mailAccountId={} messageId={}",
                    context.mailAccountId(), context.messageId());
            return;
        }
        if (!replyDraftSuggestionQueryService.isEligible(
                mailAccount.getId(),
                event.gmailThreadId(),
                context.threadMessageCount()
        )) {
            log.debug(
                    "Reply draft suggestion skipped because message is not eligible. mailAccountId={} gmailThreadId={} messageId={} threadMessageCount={}",
                    context.mailAccountId(),
                    event.gmailThreadId(),
                    context.messageId(),
                    context.threadMessageCount()
            );
            return;
        }
        log.info("Reply draft suggestion publish requested. mailAccountId={} messageId={} gmailThreadId={}",
                context.mailAccountId(), context.messageId(), event.gmailThreadId());
        replyDraftSuggestionPublisher.publish(new ReplyDraftSuggestionMessage(context.messageId()));
    }

    private boolean isSentToConnectedMailAccount(MailAccount mailAccount, List<String> toAddresses) {
        if (mailAccount == null || mailAccount.getEmailAddress() == null || toAddresses == null || toAddresses.isEmpty()) {
            return false;
        }
        String connectedEmail = mailAccount.getEmailAddress().trim();
        return toAddresses.stream()
                .filter(address -> address != null && !address.isBlank())
                .anyMatch(address -> connectedEmail.equalsIgnoreCase(address.trim()));
    }

    private LabelApplyResult applyLabelsAndResolveResult(NewMailPushContext context, List<Label> activeLabels) {
        try {
            Message message = labelQueryService.findActiveMessageWithLabelsById(context.messageId())
                    .orElse(null);

            if (message == null) {
                log.warn("Message not found for labeling: messageId={}", context.messageId());
                return LabelApplyResult.empty();
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
            return new LabelApplyResult(resolveNotification(matchedLabels), hasSensitiveLabel(matchedLabels));

        } catch (Exception e) {
            log.warn("Label apply failed for messageId={} - falling back to send FCM push.", context.messageId(), e);
            return new LabelApplyResult(true, true);
        }
    }

    private boolean hasSensitiveLabel(List<Label> matchedLabels) {
        return matchedLabels.stream().anyMatch(Label::isSensitive);
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
            log.info("FCM new mail push sent. mailAccountId={} messageId={} threadId={}",
                    context.mailAccountId(), context.messageId(), context.threadId());
        } catch (Exception e) {
            log.warn("FCM push skipped due to unexpected error. mailAccountId={} messageId={}",
                    context.mailAccountId(), context.messageId(), e);
        }
    }

    private record LabelApplyResult(boolean notificationShouldSend, boolean hasSensitiveLabel) {

        private static LabelApplyResult empty() {
            return new LabelApplyResult(true, false);
        }
    }
}
