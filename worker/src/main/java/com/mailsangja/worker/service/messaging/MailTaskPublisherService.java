package com.mailsangja.worker.service.messaging;

import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;
import com.mailsangja.worker.config.properties.WatchRenewalRabbitProperties;
import com.mailsangja.worker.dto.mail.WatchRenewalMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailTaskPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final WatchRenewalRabbitProperties watchRenewalRabbitProperties;

    public void publishWatchRenewal(WatchRenewalMessage message) {
        validateWatchRenewalProperties();
        validateWatchRenewalMessage(message);

        try {
            rabbitTemplate.convertAndSend(
                    watchRenewalRabbitProperties.getExchange(),
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

    private void validateWatchRenewalMessage(WatchRenewalMessage message) {
        if (message == null
                || message.mailAccountId() == null
                || message.userId() == null
                || isBlank(message.provider())
                || isBlank(message.emailAddress())
                || !MailProvider.GMAIL.name().equals(message.provider())
                || isBlank(watchRenewalRabbitProperties.getExchange())
                || isBlank(watchRenewalRabbitProperties.getRoutingKey())) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST);
        }
    }

    private void validateWatchRenewalProperties() {
        if (isBlank(watchRenewalRabbitProperties.getExchange())
                || isBlank(watchRenewalRabbitProperties.getQueue())
                || isBlank(watchRenewalRabbitProperties.getRoutingKey())
                || isBlank(watchRenewalRabbitProperties.getDeadLetterExchange())
                || isBlank(watchRenewalRabbitProperties.getDeadLetterQueue())
                || isBlank(watchRenewalRabbitProperties.getDeadLetterRoutingKey())
                || watchRenewalRabbitProperties.getRetryMaxAttempts() == null
                || watchRenewalRabbitProperties.getRetryMaxAttempts() < 0
                || watchRenewalRabbitProperties.getConcurrency() == null
                || watchRenewalRabbitProperties.getConcurrency() <= 0) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    "mailsangja.rabbitmq.watch-renewal properties are invalid."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
