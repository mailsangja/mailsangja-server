package com.mailsangja.worker.messaging.publisher;

import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailHistoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;

    public void publishAll(List<GmailHistoryEvent> events) {
        events.forEach(this::publish);
    }

    private void publish(GmailHistoryEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    mailTaskRabbitProperties.getExchange(),
                    event.eventType().getRoutingKey(),
                    event,
                    new CorrelationData(event.mailAccountId() + ":" + event.gmailMessageId())
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish gmail history event. eventType={} mailAccountId={} gmailMessageId={}",
                    event.eventType(),
                    event.mailAccountId(),
                    event.gmailMessageId(),
                    e
            );
        }
    }
}
