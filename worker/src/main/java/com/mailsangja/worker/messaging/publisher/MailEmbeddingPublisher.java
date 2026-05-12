package com.mailsangja.worker.messaging.publisher;

import com.mailsangja.worker.config.properties.MailEmbeddingRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailEmbeddingPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final MailEmbeddingRabbitProperties mailEmbeddingRabbitProperties;

    public void publish(MailEmbeddingMessage message) {
        rabbitTemplate.convertAndSend(
                mailTaskRabbitProperties.getExchange(),
                mailEmbeddingRabbitProperties.getRoutingKey(),
                message,
                new CorrelationData(message.messageId().toString())
        );
    }
}
