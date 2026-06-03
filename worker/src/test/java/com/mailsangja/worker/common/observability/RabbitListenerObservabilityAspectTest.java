package com.mailsangja.worker.common.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitListenerObservabilityAspectTest {

    private final RabbitListenerObservabilityAspect aspect =
            new RabbitListenerObservabilityAspect(new ObservabilitySupport());

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void openRabbitListenerScope_rawMessage가있으면MdcScope를열고Proceed한다() throws Throwable {
        MessageProperties properties = new MessageProperties();
        properties.setHeader(ObservabilitySupport.WORK_ID, "work-1");
        properties.setReceivedRoutingKey("mail.history.added");
        properties.setConsumerQueue("mailsangja.history.added");
        Message rawMessage = new Message(new byte[0], properties);
        UUID mailAccountId = UUID.randomUUID();
        Payload payload = new Payload(mailAccountId, "message-1");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload, rawMessage});
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            assertEquals("work-1", MDC.get(ObservabilitySupport.WORK_ID));
            assertEquals(mailAccountId.toString(), MDC.get(ObservabilitySupport.MAIL_ACCOUNT_ID));
            assertEquals("message-1", MDC.get(ObservabilitySupport.GMAIL_MESSAGE_ID));
            assertEquals("mail.history.added", MDC.get(ObservabilitySupport.ROUTING_KEY));
            assertEquals("mailsangja.history.added", MDC.get(ObservabilitySupport.QUEUE));
            return "ok";
        });

        Object result = aspect.openRabbitListenerScope(joinPoint);

        assertEquals("ok", result);
        assertNull(MDC.get(ObservabilitySupport.WORK_ID));
        verify(joinPoint).proceed();
    }

    @Test
    void openRabbitListenerScope_rawMessage가없으면Scope없이Proceed한다() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{new Payload(UUID.randomUUID(), "message-1")});
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.openRabbitListenerScope(joinPoint);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    private record Payload(UUID mailAccountId, String gmailMessageId) {
    }
}
