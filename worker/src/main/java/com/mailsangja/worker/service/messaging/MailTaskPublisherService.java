package com.mailsangja.worker.service.messaging;

import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;
import com.mailsangja.worker.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.WatchRenewalRabbitProperties;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadBatchMessage;
import com.mailsangja.worker.dto.mail.WatchRenewalMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailTaskPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final WatchRenewalRabbitProperties watchRenewalRabbitProperties;
    private final InitialMailSyncRabbitProperties initialMailSyncRabbitProperties;

    public void publishInitialMailSyncThreadBatch(InitialMailSyncThreadBatchMessage message) {
        validateInitialMailSyncThreadBatchProperties();
        validateInitialMailSyncThreadBatchMessage(message);

        try {
            rabbitTemplate.convertAndSend(
                    mailTaskRabbitProperties.getExchange(),
                    initialMailSyncRabbitProperties.getThreadBatchRoutingKey(),
                    message,
                    new CorrelationData(message.mailAccountId() + ":" + message.threadIds().hashCode())
            );

            log.info(
                    "Published initial mail sync thread batch for mailAccountId={} userId={} emailAddress={} threadCount={}",
                    message.mailAccountId(),
                    message.userId(),
                    message.emailAddress(),
                    message.threadIds().size()
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish initial mail sync thread batch for mailAccountId={} userId={} emailAddress={} threadCount={}",
                    message.mailAccountId(),
                    message.userId(),
                    message.emailAddress(),
                    message.threadIds().size(),
                    e
            );
        }
    }

    public void publishWatchRenewal(WatchRenewalMessage message) {
        validateWatchRenewalProperties();
        validateWatchRenewalMessage(message);

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

    private void validateWatchRenewalMessage(WatchRenewalMessage message) {
        if (message == null
                || message.mailAccountId() == null
                || message.userId() == null
                || isBlank(message.provider())
                || isBlank(message.emailAddress())
                || !MailProvider.GMAIL.name().equals(message.provider())
                || isBlank(watchRenewalRabbitProperties.getTaskName())
                || isBlank(mailTaskRabbitProperties.getExchange())
                || isBlank(watchRenewalRabbitProperties.getRoutingKey())) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST);
        }
    }

    private void validateInitialMailSyncThreadBatchMessage(InitialMailSyncThreadBatchMessage message) {
        if (message == null
                || message.mailAccountId() == null
                || message.userId() == null
                || isBlank(message.provider())
                || isBlank(message.emailAddress())
                || !MailProvider.GMAIL.name().equals(message.provider())
                || hasBlankThreadId(message.threadIds())
                || isBlank(initialMailSyncRabbitProperties.getThreadBatchTaskName())
                || isBlank(mailTaskRabbitProperties.getExchange())
                || isBlank(initialMailSyncRabbitProperties.getThreadBatchRoutingKey())) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }
    }

    private void validateWatchRenewalProperties() {
        if (isBlank(mailTaskRabbitProperties.getExchange())
                || isBlank(watchRenewalRabbitProperties.getQueueName())
                || isBlank(watchRenewalRabbitProperties.getRoutingKey())
                || isBlank(mailTaskRabbitProperties.getDeadLetterExchange())
                || isBlank(watchRenewalRabbitProperties.getDeadLetterQueueName())
                || isBlank(watchRenewalRabbitProperties.getDeadLetterRoutingKey())
                || mailTaskRabbitProperties.getRetryMaxAttempts() == null
                || mailTaskRabbitProperties.getRetryMaxAttempts() < 0
                || mailTaskRabbitProperties.getConcurrency() == null
                || mailTaskRabbitProperties.getConcurrency() <= 0) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    "mailsangja.rabbitmq.task or mailsangja.rabbitmq.watch-renewal properties are invalid."
            );
        }
    }

    private void validateInitialMailSyncThreadBatchProperties() {
        if (isBlank(mailTaskRabbitProperties.getExchange())
                || isBlank(initialMailSyncRabbitProperties.getThreadBatchTaskName())
                || isBlank(initialMailSyncRabbitProperties.getThreadBatchQueueName())
                || isBlank(initialMailSyncRabbitProperties.getThreadBatchRoutingKey())
                || isBlank(mailTaskRabbitProperties.getDeadLetterExchange())
                || isBlank(initialMailSyncRabbitProperties.getThreadBatchDeadLetterQueueName())
                || isBlank(initialMailSyncRabbitProperties.getThreadBatchDeadLetterRoutingKey())
                || mailTaskRabbitProperties.getRetryMaxAttempts() == null
                || mailTaskRabbitProperties.getRetryMaxAttempts() < 0
                || mailTaskRabbitProperties.getConcurrency() == null
                || mailTaskRabbitProperties.getConcurrency() <= 0) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    "mailsangja.rabbitmq.task or mailsangja.rabbitmq.initial-mail-sync properties are invalid."
            );
        }
    }

    private boolean hasBlankThreadId(List<String> threadIds) {
        return threadIds == null
                || threadIds.isEmpty()
                || threadIds.stream().anyMatch(this::isBlank);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
