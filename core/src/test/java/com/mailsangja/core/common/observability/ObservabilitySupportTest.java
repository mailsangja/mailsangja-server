package com.mailsangja.core.common.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ObservabilitySupportTest {

    private final ObservabilitySupport observabilitySupport = new ObservabilitySupport();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void openScope_allowedKeys만Mdc에넣고종료시이전값을복원한다() {
        MDC.put(ObservabilitySupport.WORK_ID, "previous-work");

        try (ObservabilitySupport.Scope ignored = observabilitySupport.openScope(Map.of(
                ObservabilitySupport.WORK_ID, "next-work",
                ObservabilitySupport.MAIL_ACCOUNT_ID, UUID.randomUUID(),
                "emailAddress", "user@example.com"
        ))) {
            assertEquals("next-work", MDC.get(ObservabilitySupport.WORK_ID));
            assertNotNull(MDC.get(ObservabilitySupport.MAIL_ACCOUNT_ID));
            assertNull(MDC.get("emailAddress"));
        }

        assertEquals("previous-work", MDC.get(ObservabilitySupport.WORK_ID));
        assertNull(MDC.get(ObservabilitySupport.MAIL_ACCOUNT_ID));
    }

    @Test
    void rabbitHeaders_mdc의허용된키만메시지헤더로복사한다() {
        MDC.put(ObservabilitySupport.WORK_ID, "work-1");
        MDC.put(ObservabilitySupport.USER_ID, "user-1");
        MDC.put("subject", "secret subject");
        Message message = new Message(new byte[0], new MessageProperties());

        Message processed = observabilitySupport.rabbitHeaders().postProcessMessage(message);

        assertEquals("work-1", processed.getMessageProperties().getHeaders().get(ObservabilitySupport.WORK_ID));
        assertEquals("user-1", processed.getMessageProperties().getHeaders().get(ObservabilitySupport.USER_ID));
        assertNull(processed.getMessageProperties().getHeaders().get("subject"));
    }

    @Test
    void openRabbitScope_헤더와Payload에서식별자를복원한다() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader(ObservabilitySupport.WORK_ID, "work-1");
        properties.setHeader(ObservabilitySupport.USER_ID, "user-header");
        properties.setReceivedRoutingKey("mail.history.added");
        properties.setConsumerQueue("mailsangja.history.added");
        Message message = new Message(new byte[0], properties);
        UUID mailAccountId = UUID.randomUUID();
        Payload payload = new Payload(mailAccountId, "message-1", "thread-1", EventType.MESSAGE_ADDED);

        try (ObservabilitySupport.Scope ignored = observabilitySupport.openRabbitScope(message, payload)) {
            assertEquals("work-1", MDC.get(ObservabilitySupport.WORK_ID));
            assertEquals("user-header", MDC.get(ObservabilitySupport.USER_ID));
            assertEquals(mailAccountId.toString(), MDC.get(ObservabilitySupport.MAIL_ACCOUNT_ID));
            assertEquals("message-1", MDC.get(ObservabilitySupport.GMAIL_MESSAGE_ID));
            assertEquals("thread-1", MDC.get(ObservabilitySupport.GMAIL_THREAD_ID));
            assertEquals("MESSAGE_ADDED", MDC.get(ObservabilitySupport.EVENT_TYPE));
            assertEquals("mail.history.added", MDC.get(ObservabilitySupport.ROUTING_KEY));
            assertEquals("mailsangja.history.added", MDC.get(ObservabilitySupport.QUEUE));
        }

        assertNull(MDC.get(ObservabilitySupport.WORK_ID));
    }

    private record Payload(
            UUID mailAccountId,
            String gmailMessageId,
            String gmailThreadId,
            EventType eventType
    ) {
    }

    private enum EventType {
        MESSAGE_ADDED
    }
}
