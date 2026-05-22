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

    @Test
    void classify_labelsRemovedUnread이면읽음이벤트로분류한다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-2",
                List.of(new GoogleMailHistoryItemResult(
                        "history-1",
                        List.of(),
                        List.of(new GoogleMailHistoryLabelChangeResult("message-1", "thread-1", List.of("UNREAD"))),
                        List.of(),
                        List.of()
                ))
        );

        List<GmailHistoryEvent> events = classifier.classify(mailAccount, historyResult);

        assertEquals(1, events.size());
        assertEquals(GmailHistoryEventType.MESSAGE_READ, events.getFirst().eventType());
        assertEquals(mailAccountId, events.getFirst().mailAccountId());
    }

    @Test
    void classify_labelsAddedUnread이면안읽음이벤트로분류한다() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-2",
                List.of(new GoogleMailHistoryItemResult(
                        "history-1",
                        List.of(new GoogleMailHistoryLabelChangeResult("message-1", "thread-1", List.of("UNREAD"))),
                        List.of(),
                        List.of(),
                        List.of()
                ))
        );

        List<GmailHistoryEvent> events = classifier.classify(mailAccount, historyResult);

        assertEquals(1, events.size());
        assertEquals(GmailHistoryEventType.MESSAGE_UNREAD, events.getFirst().eventType());
    }

    @Test
    void classify_trashLabel추가와제거를휴지통삭제와복원으로분류한다() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-2",
                List.of(new GoogleMailHistoryItemResult(
                        "history-1",
                        List.of(new GoogleMailHistoryLabelChangeResult("message-trash", "thread-1", List.of("TRASH"))),
                        List.of(new GoogleMailHistoryLabelChangeResult("message-restore", "thread-2", List.of("TRASH"))),
                        List.of(),
                        List.of()
                ))
        );

        List<GmailHistoryEvent> events = classifier.classify(mailAccount, historyResult);

        assertEquals(2, events.size());
        assertEquals(GmailHistoryEventType.MESSAGE_TRASHED, events.get(0).eventType());
        assertEquals("message-trash", events.get(0).gmailMessageId());
        assertEquals(GmailHistoryEventType.MESSAGE_RESTORED, events.get(1).eventType());
        assertEquals("message-restore", events.get(1).gmailMessageId());
    }

    @Test
    void classify_messagesDeleted는영구삭제로분류한다() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-2",
                List.of(new GoogleMailHistoryItemResult(
                        "history-1",
                        List.of(),
                        List.of(),
                        List.of(new GoogleMailHistoryLabelChangeResult("message-1", "thread-1", List.of())),
                        List.of()
                ))
        );

        List<GmailHistoryEvent> events = classifier.classify(mailAccount, historyResult);

        assertEquals(1, events.size());
        assertEquals(GmailHistoryEventType.MESSAGE_PERMANENTLY_DELETED, events.getFirst().eventType());
    }

    @Test
    void classify_같은메시지와이벤트타입은마지막이벤트로중복제거한다() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-3",
                List.of(
                        new GoogleMailHistoryItemResult(
                                "history-1",
                                List.of(new GoogleMailHistoryLabelChangeResult("message-1", "thread-old", List.of("UNREAD"))),
                                List.of(),
                                List.of(),
                                List.of()
                        ),
                        new GoogleMailHistoryItemResult(
                                "history-2",
                                List.of(new GoogleMailHistoryLabelChangeResult("message-1", "thread-new", List.of("UNREAD"))),
                                List.of(),
                                List.of(),
                                List.of()
                        )
                )
        );

        List<GmailHistoryEvent> events = classifier.classify(mailAccount, historyResult);

        assertEquals(1, events.size());
        assertEquals(GmailHistoryEventType.MESSAGE_UNREAD, events.getFirst().eventType());
        assertEquals("thread-new", events.getFirst().gmailThreadId());
        assertEquals("history-2", events.getFirst().historyId());
    }

    @Test
    void classify_null입력과잘못된이벤트는빈목록을반환한다() {
        assertTrue(classifier.classify(null, null).isEmpty());

        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
        GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult(
                "history-2",
                List.of(new GoogleMailHistoryItemResult(
                        "history-1",
                        List.of(new GoogleMailHistoryLabelChangeResult("", "thread-1", List.of("UNREAD"))),
                        List.of(new GoogleMailHistoryLabelChangeResult("message-1", null, List.of("UNREAD"))),
                        List.of(new GoogleMailHistoryLabelChangeResult(null, "thread-2", List.of())),
                        List.of(new GoogleMailHistoryMessageAddedResult(" ", "thread-3", List.of("INBOX")))
                ))
        );

        assertTrue(classifier.classify(mailAccount, historyResult).isEmpty());
    }
}
