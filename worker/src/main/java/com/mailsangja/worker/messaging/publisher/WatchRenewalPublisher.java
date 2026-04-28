package com.mailsangja.worker.messaging.publisher;

import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.WatchRenewalRabbitProperties;
import com.mailsangja.worker.dto.mail.watch.WatchRenewalMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchRenewalPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final WatchRenewalRabbitProperties watchRenewalRabbitProperties;

    public void publish(WatchRenewalMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    mailTaskRabbitProperties.getExchange(),
                    watchRenewalRabbitProperties.getRoutingKey(),
                    message,
                    new CorrelationData(message.mailAccountId().toString())
            );
            log.info(
                    "Published watch renewal request for mailAccountId={} userId={} provider={} emailAddress={}",
                    message.mailAccountId(),
                    message.userId(),
                    message.provider(),
                    message.emailAddress()
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish watch renewal request for mailAccountId={} userId={} provider={} emailAddress={}",
                    message.mailAccountId(),
                    message.userId(),
                    message.provider(),
                    message.emailAddress(),
                    e
            );
        }
    }
}
