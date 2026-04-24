package com.mailsangja.worker.messaging.publisher;

import com.mailsangja.worker.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadBatchMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialMailSyncThreadBatchPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final InitialMailSyncRabbitProperties initialMailSyncRabbitProperties;

    public void publish(InitialMailSyncThreadBatchMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    mailTaskRabbitProperties.getExchange(),
                    initialMailSyncRabbitProperties.getThreadBatchRoutingKey(),
                    message,
                    new CorrelationData(message.mailAccountId() + ":" + message.threadIds().hashCode())
            );
            log.info(
                    "Published initial mail sync thread batch. mailAccountId={} emailAddress={} threadCount={}",
                    message.mailAccountId(),
                    message.emailAddress(),
                    message.threadIds().size()
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish initial mail sync thread batch. mailAccountId={} emailAddress={} threadCount={}",
                    message.mailAccountId(),
                    message.emailAddress(),
                    message.threadIds().size(),
                    e
            );
        }
    }
}
