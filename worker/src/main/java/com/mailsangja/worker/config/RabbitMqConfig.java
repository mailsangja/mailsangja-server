package com.mailsangja.worker.config;

import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;
import com.mailsangja.worker.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.WatchRenewalRabbitProperties;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
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
                .ttl(toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
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
    public Queue initialMailSyncThreadBatchQueue(
            InitialMailSyncRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        validateTaskName(properties.getThreadBatchTaskName(), "mailsangja.rabbitmq.initial-mail-sync.thread-batch-task-name");

        return QueueBuilder.durable(properties.getThreadBatchQueueName())
                .ttl(toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getThreadBatchDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue initialMailSyncThreadBatchDeadLetterQueue(InitialMailSyncRabbitProperties properties) {
        validateTaskName(properties.getThreadBatchTaskName(), "mailsangja.rabbitmq.initial-mail-sync.thread-batch-task-name");
        return QueueBuilder.durable(properties.getThreadBatchDeadLetterQueueName()).build();
    }

    @Bean
    public Queue watchRenewalQueue(
            WatchRenewalRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        validateTaskName(properties.getTaskName(), "mailsangja.rabbitmq.watch-renewal.task-name");

        return QueueBuilder.durable(properties.getQueueName())
                .ttl(toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue watchRenewalDeadLetterQueue(WatchRenewalRabbitProperties properties) {
        validateTaskName(properties.getTaskName(), "mailsangja.rabbitmq.watch-renewal.task-name");
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
    public Binding initialMailSyncThreadBatchBinding(
            @Qualifier("initialMailSyncThreadBatchQueue") Queue initialMailSyncThreadBatchQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            InitialMailSyncRabbitProperties properties
    ) {
        return BindingBuilder.bind(initialMailSyncThreadBatchQueue)
                .to(mailTaskExchange)
                .with(properties.getThreadBatchRoutingKey());
    }

    @Bean
    public Binding initialMailSyncThreadBatchDeadLetterBinding(
            @Qualifier("initialMailSyncThreadBatchDeadLetterQueue") Queue initialMailSyncThreadBatchDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            InitialMailSyncRabbitProperties properties
    ) {
        return BindingBuilder.bind(initialMailSyncThreadBatchDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getThreadBatchDeadLetterRoutingKey());
    }

    @Bean
    public Binding watchRenewalBinding(
            @Qualifier("watchRenewalQueue") Queue watchRenewalQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            WatchRenewalRabbitProperties properties
    ) {
        return BindingBuilder.bind(watchRenewalQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding watchRenewalDeadLetterBinding(
            @Qualifier("watchRenewalDeadLetterQueue") Queue watchRenewalDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            WatchRenewalRabbitProperties properties
    ) {
        return BindingBuilder.bind(watchRenewalDeadLetterQueue)
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
        return rabbitTemplate;
    }

    @Bean
    public MessageRecoverer watchRenewalMessageRecoverer(WatchRenewalRabbitProperties properties) {
        return (message, cause) -> {
            log.warn(
                    "Watch renewal message retries exhausted. Sending to DLQ routingKey={} messageBody={}",
                    properties.getDeadLetterRoutingKey(),
                    new String(message.getBody()),
                    cause
            );
            throw new AmqpRejectAndDontRequeueException("Watch renewal retries exhausted", cause);
        };
    }

    @Bean
    public MessageRecoverer initialMailSyncMessageRecoverer(InitialMailSyncRabbitProperties properties) {
        return (message, cause) -> {
            log.warn(
                    "Initial mail sync message retries exhausted. Sending to DLQ routingKey={} messageBody={}",
                    properties.getDeadLetterRoutingKey(),
                    new String(message.getBody()),
                    cause
            );
            throw new AmqpRejectAndDontRequeueException("Initial mail sync retries exhausted", cause);
        };
    }

    @Bean
    public MessageRecoverer initialMailSyncThreadBatchMessageRecoverer(InitialMailSyncRabbitProperties properties) {
        return (message, cause) -> {
            log.warn(
                    "Initial mail sync thread batch retries exhausted. Sending to DLQ routingKey={} messageBody={}",
                    properties.getThreadBatchDeadLetterRoutingKey(),
                    new String(message.getBody()),
                    cause
            );
            throw new AmqpRejectAndDontRequeueException("Initial mail sync thread batch retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor rabbitRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("initialMailSyncMessageRecoverer") MessageRecoverer initialMailSyncMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(properties.getRetryMaxAttempts())
                .recoverer(initialMailSyncMessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("rabbitRetryInterceptor") MethodInterceptor rabbitRetryInterceptor,
            MailTaskRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(rabbitRetryInterceptor);
        return factory;
    }

    @Bean
    public MethodInterceptor initialMailSyncThreadBatchRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("initialMailSyncThreadBatchMessageRecoverer") MessageRecoverer initialMailSyncThreadBatchMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(properties.getRetryMaxAttempts())
                .recoverer(initialMailSyncThreadBatchMessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory initialMailSyncThreadBatchRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("initialMailSyncThreadBatchRetryInterceptor") MethodInterceptor initialMailSyncThreadBatchRetryInterceptor,
            MailTaskRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(initialMailSyncThreadBatchRetryInterceptor);
        return factory;
    }

    @Bean
    public MethodInterceptor watchRenewalRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("watchRenewalMessageRecoverer") MessageRecoverer watchRenewalMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(properties.getRetryMaxAttempts())
                .recoverer(watchRenewalMessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory watchRenewalRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("watchRenewalRetryInterceptor") MethodInterceptor watchRenewalRetryInterceptor,
            MailTaskRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(watchRenewalRetryInterceptor);
        return factory;
    }

    private int toQueueTtlMillis(Duration ttl, String propertyName) {
        if (ttl == null) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    propertyName + " must not be null."
            );
        }

        if (ttl.isNegative()) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    propertyName + " must be greater than or equal to 0."
            );
        }

        long ttlMillis = ttl.toMillis();
        if (ttlMillis > MAX_QUEUE_TTL_MILLIS) {
            throw new MqException(
                    MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL,
                    propertyName + " must be less than or equal to "
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
