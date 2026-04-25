package com.mailsangja.db.entity.mail;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTest {

    @Test
    void from_답장관련헤더를저장한다() {
        // given
        Thread thread = createThread();
        Message.CreateValues values = createValues(
                "<message-id@example.com>",
                "<root-message@example.com>",
                "<parent-message@example.com>",
                "\"Reply Alias\" <reply@example.com>"
        );

        // when
        Message message = Message.from(thread, values);

        // then
        assertEquals("<message-id@example.com>", message.getRfcMessageId());
        assertEquals("<root-message@example.com>", message.getReferencesHeader());
        assertEquals("<parent-message@example.com>", message.getInReplyToHeader());
        assertEquals("\"Reply Alias\" <reply@example.com>", message.getReplyToHeader());
    }

    @Test
    void updateFrom_답장관련헤더를갱신한다() {
        // given
        Message message = Message.from(createThread(), createValues(
                "<old-message-id@example.com>",
                "<old-root-message@example.com>",
                "<old-parent-message@example.com>",
                "\"Old Reply\" <old-reply@example.com>"
        ));
        Message.CreateValues updateValues = createValues(
                "<new-message-id@example.com>",
                "<new-root-message@example.com>",
                "<new-parent-message@example.com>",
                "\"New Reply\" <new-reply@example.com>"
        );

        // when
        message.updateFrom(updateValues);

        // then
        assertEquals("<new-message-id@example.com>", message.getRfcMessageId());
        assertEquals("<new-root-message@example.com>", message.getReferencesHeader());
        assertEquals("<new-parent-message@example.com>", message.getInReplyToHeader());
        assertEquals("\"New Reply\" <new-reply@example.com>", message.getReplyToHeader());
    }

    private Thread createThread() {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(MailAccount.builder().id(UUID.randomUUID()).build())
                .gmailThreadId("gmail-thread-id")
                .direction(Direction.INBOUND)
                .read(true)
                .messageCount(1)
                .build();
    }

    private Message.CreateValues createValues(
            String rfcMessageId,
            String referencesHeader,
            String inReplyToHeader,
            String replyToHeader
    ) {
        return new Message.CreateValues(
                "gmail-message-id",
                rfcMessageId,
                referencesHeader,
                inReplyToHeader,
                replyToHeader,
                Direction.INBOUND,
                "subject",
                "from@example.com",
                "From",
                List.of("to@example.com"),
                List.of("To"),
                List.of(),
                List.of(),
                "snippet",
                true,
                LocalDateTime.of(2026, 4, 25, 10, 0),
                "body text",
                null
        );
    }
}
