package com.mailsangja.worker.messaging.publisher;

import com.mailsangja.worker.config.properties.MailEmbeddingRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MailEmbeddingPublisherTest {

    @Test
    void publish_messageId를correlationData로사용해임베딩메시지를발행한다() {
        // given
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MailTaskRabbitProperties mailTaskRabbitProperties = createMailTaskRabbitProperties();
        MailEmbeddingRabbitProperties mailEmbeddingRabbitProperties = new MailEmbeddingRabbitProperties();
        MailEmbeddingPublisher publisher = new MailEmbeddingPublisher(
                rabbitTemplate,
                mailTaskRabbitProperties,
                mailEmbeddingRabbitProperties
        );
        UUID messageId = UUID.randomUUID();
        MailEmbeddingMessage message = new MailEmbeddingMessage(messageId);

        // when
        publisher.publish(message);

        // then
        ArgumentCaptor<CorrelationData> correlationDataCaptor = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(
                eq("mailsangja.mail.task"),
                eq("mail.embedding"),
                eq(message),
                correlationDataCaptor.capture()
        );
        assertEquals(messageId.toString(), correlationDataCaptor.getValue().getId());
    }

    @Test
    void publish_RabbitTemplate발행실패를전파한다() {
        // given
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MailTaskRabbitProperties mailTaskRabbitProperties = createMailTaskRabbitProperties();
        MailEmbeddingRabbitProperties mailEmbeddingRabbitProperties = new MailEmbeddingRabbitProperties();
        MailEmbeddingPublisher publisher = new MailEmbeddingPublisher(
                rabbitTemplate,
                mailTaskRabbitProperties,
                mailEmbeddingRabbitProperties
        );
        MailEmbeddingMessage message = new MailEmbeddingMessage(UUID.randomUUID());
        AmqpException publishFailure = new AmqpException("publish failed");
        doThrow(publishFailure).when(rabbitTemplate).convertAndSend(
                eq("mailsangja.mail.task"),
                eq("mail.embedding"),
                eq(message),
                any(CorrelationData.class)
        );

        // when
        AmqpException exception = assertThrows(AmqpException.class, () -> publisher.publish(message));

        // then
        assertEquals(publishFailure, exception);
    }

    private MailTaskRabbitProperties createMailTaskRabbitProperties() {
        MailTaskRabbitProperties properties = new MailTaskRabbitProperties();
        properties.setExchange("mailsangja.mail.task");
        return properties;
    }
}
