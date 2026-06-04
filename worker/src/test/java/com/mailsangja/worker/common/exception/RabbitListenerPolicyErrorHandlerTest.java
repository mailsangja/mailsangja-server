package com.mailsangja.worker.common.exception;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RabbitListenerPolicyErrorHandlerTest {

    private final RabbitListenerPolicyErrorHandler errorHandler = new RabbitListenerPolicyErrorHandler();

    @Test
    void handleError_ack정책에등록된예외는삼키고null을반환한다() {
        ListenerExecutionFailedException exception = listenerException(
                new MailPushException(MailPushErrorCode.GOOGLE_REFRESH_TOKEN_MISSING)
        );

        Object result = assertDoesNotThrow(() -> errorHandler.handleError(
                amqpMessage(),
                null,
                MessageBuilder.withPayload("payload").build(),
                exception
        ));

        assertNull(result);
    }

    @Test
    void handleError_다른예외는다시던진다() {
        ListenerExecutionFailedException exception = listenerException(
                new MailPushException(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED)
        );

        assertThrows(
                ListenerExecutionFailedException.class,
                () -> errorHandler.handleError(amqpMessage(), null, MessageBuilder.withPayload("payload").build(), exception)
        );
    }

    private ListenerExecutionFailedException listenerException(Throwable cause) {
        return new ListenerExecutionFailedException("listener failed", cause, amqpMessage());
    }

    private Message amqpMessage() {
        MessageProperties properties = new MessageProperties();
        properties.setConsumerQueue("test.queue");
        properties.setMessageId("message-id");
        return new Message(new byte[0], properties);
    }
}
