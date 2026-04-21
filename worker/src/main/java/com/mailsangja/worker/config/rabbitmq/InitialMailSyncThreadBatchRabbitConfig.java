package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class InitialMailSyncThreadBatchRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(InitialMailSyncThreadBatchRabbitConfig.class);

    @Bean
    public Queue initialMailSyncThreadBatchQueue(
            InitialMailSyncRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        RabbitMqConfig.validateTaskName(properties.getThreadBatchTaskName(), "mailsangja.rabbitmq.initial-mail-sync.thread-batch-task-name");
        return QueueBuilder.durable(properties.getThreadBatchQueueName())
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getThreadBatchDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue initialMailSyncThreadBatchDeadLetterQueue(InitialMailSyncRabbitProperties properties) {
        RabbitMqConfig.validateTaskName(properties.getThreadBatchTaskName(), "mailsangja.rabbitmq.initial-mail-sync.thread-batch-task-name");
        return QueueBuilder.durable(properties.getThreadBatchDeadLetterQueueName()).build();
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

}
