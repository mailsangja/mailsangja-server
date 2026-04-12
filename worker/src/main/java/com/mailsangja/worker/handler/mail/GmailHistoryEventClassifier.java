package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.GmailHistoryEventType;
import com.mailsangja.worker.dto.gmail.GoogleMailHistoryItemResult;
import com.mailsangja.worker.dto.gmail.GoogleMailHistoryLabelChangeResult;
import com.mailsangja.worker.dto.gmail.GoogleMailHistoryListResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GmailHistoryEventClassifier {

    private static final String UNREAD_LABEL_ID = "UNREAD";

    public List<GmailHistoryEvent> classify(MailAccount mailAccount, GoogleMailHistoryListResult historyResult) {
        if (mailAccount == null || historyResult == null || historyResult.histories() == null) {
            return List.of();
        }

        Map<String, GmailHistoryEvent> deduplicatedEvents = new LinkedHashMap<>();

        for (GoogleMailHistoryItemResult historyItem : historyResult.histories()) {
            classifyLabelChanges(
                    deduplicatedEvents,
                    mailAccount,
                    historyItem == null ? null : historyItem.historyId(),
                    historyItem == null ? List.of() : historyItem.labelsRemoved(),
                    GmailHistoryEventType.MESSAGE_READ
            );
            classifyLabelChanges(
                    deduplicatedEvents,
                    mailAccount,
                    historyItem == null ? null : historyItem.historyId(),
                    historyItem == null ? List.of() : historyItem.labelsAdded(),
                    GmailHistoryEventType.MESSAGE_UNREAD
            );
        }

        return List.copyOf(deduplicatedEvents.values());
    }

    private void classifyLabelChanges(
            Map<String, GmailHistoryEvent> deduplicatedEvents,
            MailAccount mailAccount,
            String historyId,
            List<GoogleMailHistoryLabelChangeResult> labelChanges,
            GmailHistoryEventType eventType
    ) {
        if (labelChanges == null || labelChanges.isEmpty()) {
            return;
        }

        for (GoogleMailHistoryLabelChangeResult labelChange : labelChanges) {
            if (!supportsEvent(labelChange)) {
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

    private boolean supportsEvent(GoogleMailHistoryLabelChangeResult labelChange) {
        return labelChange != null
                && !isBlank(labelChange.gmailMessageId())
                && !isBlank(labelChange.gmailThreadId())
                && labelChange.labelIds() != null
                && labelChange.labelIds().contains(UNREAD_LABEL_ID);
    }

    private String buildDeduplicationKey(GmailHistoryEvent event) {
        return event.eventType() + ":" + event.gmailMessageId();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
