package com.mailsangja.core.config;

import com.mailsangja.core.common.exception.mq.MqErrorCode;
import com.mailsangja.core.common.exception.mq.MqException;
import com.mailsangja.core.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.core.config.properties.MailTaskRabbitProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Configuration
public class RabbitMqConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfig.class);
    private static final long MAX_QUEUE_TTL_MILLIS = Integer.MAX_VALUE;

    @Bean
    public DirectExchange mailTaskExchange(MailTaskRabbitProperties properties) {
        return new DirectExchange(properties.getExchange());
    }

    @Bean
    public DirectExchange mailTaskDeadLetterExchange(MailTaskRabbitProperties properties) {
        return new DirectExchange(properties.getDeadLetterExchange());
    }

    @Bean
    public Queue initialMailSyncQueue(
            InitialMailSyncRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        validateTaskName(properties.getTaskName(), "mailsangja.rabbitmq.initial-mail-sync.task-name");

        return QueueBuilder.durable(properties.getQueueName())
                .ttl(toQueueTtlMillis(mailTaskRabbitProperties.getTtl()))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue initialMailSyncDeadLetterQueue(InitialMailSyncRabbitProperties properties) {
        validateTaskName(properties.getTaskName(), "mailsangja.rabbitmq.initial-mail-sync.task-name");
        return QueueBuilder.durable(properties.getDeadLetterQueueName()).build();
    }

    @Bean
    public Binding initialMailSyncBinding(
            @Qualifier("initialMailSyncQueue") Queue initialMailSyncQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            InitialMailSyncRabbitProperties properties
    ) {
        return BindingBuilder.bind(initialMailSyncQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding initialMailSyncDeadLetterBinding(
            @Qualifier("initialMailSyncDeadLetterQueue") Queue initialMailSyncDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            InitialMailSyncRabbitProperties properties
    ) {
        return BindingBuilder.bind(initialMailSyncDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            MailTaskRabbitProperties properties
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        rabbitTemplate.setMandatory(Boolean.TRUE.equals(properties.getPublisherMandatory()));
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("Confirmed initial mail sync message publish correlationId={}",
                        correlationData == null ? null : correlationData.getId());
                return;
            }

            log.warn(
                    "Initial mail sync message publish was not confirmed by broker correlationId={} cause={}",
                    correlationData == null ? null : correlationData.getId(),
                    cause
            );
        });
        rabbitTemplate.setReturnsCallback(returned -> log.warn(
                "Initial mail sync message returned exchange={} routingKey={} replyCode={} replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        ));
        return rabbitTemplate;
    }

    private int toQueueTtlMillis(Duration ttl) {
        if (ttl == null) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    "mailsangja.rabbitmq.task.ttl must not be null."
            );
        }

        if (ttl.isNegative()) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    "mailsangja.rabbitmq.task.ttl must be greater than or equal to 0."
            );
        }

        long ttlMillis = ttl.toMillis();
        if (ttlMillis > MAX_QUEUE_TTL_MILLIS) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    "mailsangja.rabbitmq.task.ttl must be less than or equal to "
                            + MAX_QUEUE_TTL_MILLIS
                            + "ms."
            );
        }

        return (int) ttlMillis;
    }

    private void validateTaskName(String taskName, String propertyName) {
        if (taskName == null || taskName.isBlank()) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    propertyName + " must not be blank."
            );
        }
    }
}
