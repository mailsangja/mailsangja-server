package com.mailsangja.worker.messaging.publisher;

import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.PaymentWebhookRabbitProperties;
import com.mailsangja.worker.dto.payment.PaymentMessage;
import com.mailsangja.worker.dto.payment.PortOneWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWebhookPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final PaymentWebhookRabbitProperties paymentWebhookRabbitProperties;

    public void publish(PortOneWebhookRequest request) {
        PaymentMessage message = request.toMessage();

        try {
            rabbitTemplate.convertAndSend(
                    mailTaskRabbitProperties.getExchange(),
                    paymentWebhookRabbitProperties.getRoutingKey(),
                    message,
                    new CorrelationData(request.webhookId())
            );
            log.info("Payment webhook published. webhookId={} paymentId={}", request.webhookId(), request.data().paymentId());
        } catch (AmqpException e) {
            log.warn("Failed to publish payment webhook. webhookId={}", request.webhookId(), e);
            throw e;
        }
    }
}
