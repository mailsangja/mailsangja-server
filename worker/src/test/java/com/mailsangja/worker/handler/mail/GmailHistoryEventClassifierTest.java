package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryItemResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryLabelChangeResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryListResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryMessageAddedResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailHistoryEventClassifierTest {

    private final GmailHistoryEventClassifier classifier = new GmailHistoryEventClassifier();

    @Test
    void classify_skipsDraftMessageAddedEvents() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-2",
                List.of(new GoogleMailHistoryItemResult(
                        "history-1",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new GoogleMailHistoryMessageAddedResult(
                                "message-draft",
                                "thread-1",
                                List.of("DRAFT")
                        ))
                ))
        );

        List<GmailHistoryEvent> events = classifier.classify(mailAccount, historyResult);

        assertTrue(events.isEmpty());
    }

    @Test
    void classify_skipsMessageAddedWhenDraftLabelIsAddedInSameHistory() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-2",
                List.of(new GoogleMailHistoryItemResult(
                        "history-1",
                        List.of(new GoogleMailHistoryLabelChangeResult(
                                "message-draft",
                                "thread-1",
                                List.of("DRAFT")
                        )),
                        List.of(),
                        List.of(),
                        List.of(new GoogleMailHistoryMessageAddedResult(
                                "message-draft",
                                "thread-1",
                                List.of()
                        ))
                ))
        );

        List<GmailHistoryEvent> events = classifier.classify(mailAccount, historyResult);

        assertTrue(events.isEmpty());
    }

    @Test
    void classify_publishesNonDraftMessageAddedEvents() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-2",
                List.of(new GoogleMailHistoryItemResult(
                        "history-1",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new GoogleMailHistoryMessageAddedResult(
                                "message-1",
                                "thread-1",
                                List.of("INBOX")
                        ))
                ))
        );

        List<GmailHistoryEvent> events = classifier.classify(mailAccount, historyResult);

        assertEquals(1, events.size());
        GmailHistoryEvent event = events.getFirst();
        assertEquals(GmailHistoryEventType.MESSAGE_ADDED, event.eventType());
        assertEquals(mailAccountId, event.mailAccountId());
        assertEquals("message-1", event.gmailMessageId());
        assertEquals("thread-1", event.gmailThreadId());
    }
}
