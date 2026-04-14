package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryItemResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryLabelChangeResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryListResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GmailHistoryEventClassifier {

    private static final String UNREAD_LABEL_ID = "UNREAD";
    private static final String TRASH_LABEL_ID = "TRASH";

    public List<GmailHistoryEvent> classify(MailAccount mailAccount, GoogleMailHistoryListResult historyResult) {
        if (mailAccount == null || historyResult == null || historyResult.histories() == null) {
            return List.of();
        }

        Map<String, GmailHistoryEvent> deduplicatedEvents = new LinkedHashMap<>();

        for (GoogleMailHistoryItemResult historyItem : historyResult.histories()) {
            String historyId = historyItem == null ? null : historyItem.historyId();
            List<GoogleMailHistoryLabelChangeResult> labelsAdded = historyItem == null ? List.of() : historyItem.labelsAdded();
            List<GoogleMailHistoryLabelChangeResult> labelsRemoved = historyItem == null ? List.of() : historyItem.labelsRemoved();

            classifyLabelChanges(deduplicatedEvents, mailAccount, historyId, labelsRemoved, GmailHistoryEventType.MESSAGE_READ, UNREAD_LABEL_ID);
            classifyLabelChanges(deduplicatedEvents, mailAccount, historyId, labelsAdded, GmailHistoryEventType.MESSAGE_UNREAD, UNREAD_LABEL_ID);
            classifyLabelChanges(deduplicatedEvents, mailAccount, historyId, labelsAdded, GmailHistoryEventType.MESSAGE_TRASHED, TRASH_LABEL_ID);
            classifyLabelChanges(deduplicatedEvents, mailAccount, historyId, labelsRemoved, GmailHistoryEventType.MESSAGE_RESTORED, TRASH_LABEL_ID);

            List<GoogleMailHistoryLabelChangeResult> messagesDeleted = historyItem == null ? List.of() : historyItem.messagesDeleted();
            classifyPermanentlyDeletedMessages(deduplicatedEvents, mailAccount, historyId, messagesDeleted);
        }

        return List.copyOf(deduplicatedEvents.values());
    }

    private void classifyLabelChanges(
            Map<String, GmailHistoryEvent> deduplicatedEvents,
            MailAccount mailAccount,
            String historyId,
            List<GoogleMailHistoryLabelChangeResult> labelChanges,
            GmailHistoryEventType eventType,
            String targetLabelId
    ) {
        if (labelChanges == null || labelChanges.isEmpty()) {
            return;
        }

        for (GoogleMailHistoryLabelChangeResult labelChange : labelChanges) {
            if (!supportsEvent(labelChange, targetLabelId)) {
                continue;
            }

            GmailHistoryEvent event = new GmailHistoryEvent(
                    eventType,
                    mailAccount.getId(),
                    labelChange.gmailMessageId(),
                    labelChange.gmailThreadId(),
                    historyId
            );
            deduplicatedEvents.put(buildDeduplicationKey(event), event);
        }
    }

    private void classifyPermanentlyDeletedMessages(
            Map<String, GmailHistoryEvent> deduplicatedEvents,
            MailAccount mailAccount,
            String historyId,
            List<GoogleMailHistoryLabelChangeResult> messagesDeleted
    ) {
        if (messagesDeleted == null || messagesDeleted.isEmpty()) {
            return;
        }

        for (GoogleMailHistoryLabelChangeResult deletedMessage : messagesDeleted) {
            if (deletedMessage == null
                    || isBlank(deletedMessage.gmailMessageId())
                    || isBlank(deletedMessage.gmailThreadId())) {
                continue;
            }

            GmailHistoryEvent event = new GmailHistoryEvent(
                    GmailHistoryEventType.MESSAGE_PERMANENTLY_DELETED,
                    mailAccount.getId(),
                    deletedMessage.gmailMessageId(),
                    deletedMessage.gmailThreadId(),
                    historyId
            );
            deduplicatedEvents.put(buildDeduplicationKey(event), event);
        }
    }

    private boolean supportsEvent(GoogleMailHistoryLabelChangeResult labelChange, String targetLabelId) {
        return labelChange != null
                && !isBlank(labelChange.gmailMessageId())
                && !isBlank(labelChange.gmailThreadId())
                && labelChange.labelIds() != null
                && labelChange.labelIds().contains(targetLabelId);
    }

    private String buildDeduplicationKey(GmailHistoryEvent event) {
        return event.eventType() + ":" + event.gmailMessageId();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
