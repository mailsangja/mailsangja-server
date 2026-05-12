package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryItemResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryLabelChangeResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryListResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryMessageAddedResult;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GmailHistoryEventClassifier {

    private static final String UNREAD_LABEL_ID = "UNREAD";
    private static final String TRASH_LABEL_ID = "TRASH";
    private static final String DRAFT_LABEL_ID = "DRAFT";

    public List<GmailHistoryEvent> classify(MailAccount mailAccount, GoogleMailHistoryListResult historyResult) {
        if (mailAccount == null || historyResult == null || historyResult.histories() == null) {
            return List.of();
        }

        Map<String, GmailHistoryEvent> deduplicatedEvents = new LinkedHashMap<>();
        Set<String> draftMessageIds = collectDraftMessageIds(historyResult);

        for (GoogleMailHistoryItemResult historyItem : historyResult.histories()) {
            String historyId = historyItem == null ? null : historyItem.historyId();
            List<GoogleMailHistoryLabelChangeResult> labelsAdded = historyItem == null ? List.of() : historyItem.labelsAdded();
            List<GoogleMailHistoryLabelChangeResult> labelsRemoved = historyItem == null ? List.of() : historyItem.labelsRemoved();
            List<GoogleMailHistoryLabelChangeResult> messagesDeleted = historyItem == null ? List.of() : historyItem.messagesDeleted();
            List<GoogleMailHistoryMessageAddedResult> messagesAdded = historyItem == null ? List.of() : historyItem.messagesAdded();

            // 신규 메시지를 먼저 반영해 이후 READ/UNREAD 처리 시 불필요한 외부 재조회 가능성을 낮춘다.
            classifyMessagesAdded(deduplicatedEvents, mailAccount, historyId, messagesAdded, draftMessageIds);
            classifyLabelChanges(deduplicatedEvents, mailAccount, historyId, labelsRemoved, GmailHistoryEventType.MESSAGE_READ, UNREAD_LABEL_ID);
            classifyLabelChanges(deduplicatedEvents, mailAccount, historyId, labelsAdded, GmailHistoryEventType.MESSAGE_UNREAD, UNREAD_LABEL_ID);
            classifyLabelChanges(deduplicatedEvents, mailAccount, historyId, labelsAdded, GmailHistoryEventType.MESSAGE_TRASHED, TRASH_LABEL_ID);
            classifyLabelChanges(deduplicatedEvents, mailAccount, historyId, labelsRemoved, GmailHistoryEventType.MESSAGE_RESTORED, TRASH_LABEL_ID);
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

    private void classifyMessagesAdded(
            Map<String, GmailHistoryEvent> deduplicatedEvents,
            MailAccount mailAccount,
            String historyId,
            List<GoogleMailHistoryMessageAddedResult> messagesAdded,
            Set<String> draftMessageIds
    ) {
        if (messagesAdded == null || messagesAdded.isEmpty()) {
            return;
        }

        for (GoogleMailHistoryMessageAddedResult addedMessage : messagesAdded) {
            if (!supportsMessageAdded(addedMessage, draftMessageIds)) {
                continue;
            }

            GmailHistoryEvent event = new GmailHistoryEvent(
                    GmailHistoryEventType.MESSAGE_ADDED,
                    mailAccount.getId(),
                    addedMessage.gmailMessageId(),
                    addedMessage.gmailThreadId(),
                    historyId
            );
            deduplicatedEvents.put(buildDeduplicationKey(event), event);
        }
    }

    private Set<String> collectDraftMessageIds(GoogleMailHistoryListResult historyResult) {
        Set<String> draftMessageIds = new HashSet<>();
        for (GoogleMailHistoryItemResult historyItem : historyResult.histories()) {
            List<GoogleMailHistoryLabelChangeResult> labelsAdded = historyItem == null ? List.of() : historyItem.labelsAdded();
            if (labelsAdded == null || labelsAdded.isEmpty()) {
                continue;
            }

            labelsAdded.stream()
                    .filter(labelChange -> supportsEvent(labelChange, DRAFT_LABEL_ID))
                    .map(GoogleMailHistoryLabelChangeResult::gmailMessageId)
                    .forEach(draftMessageIds::add);
        }
        return draftMessageIds;
    }

    private boolean supportsMessageAdded(GoogleMailHistoryMessageAddedResult addedMessage, Set<String> draftMessageIds) {
        return addedMessage != null
                && !isBlank(addedMessage.gmailMessageId())
                && !isBlank(addedMessage.gmailThreadId())
                && !isDraftMessage(addedMessage, draftMessageIds);
    }

    private boolean isDraftMessage(GoogleMailHistoryMessageAddedResult addedMessage, Set<String> draftMessageIds) {
        return (addedMessage.labelIds() != null && addedMessage.labelIds().contains(DRAFT_LABEL_ID))
                || (draftMessageIds != null && draftMessageIds.contains(addedMessage.gmailMessageId()));
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
