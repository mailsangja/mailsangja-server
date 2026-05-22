package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.service.notification.DiscordAlertService;
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
public class InitialMailSyncRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(InitialMailSyncRabbitConfig.class);

    @Bean
    public Queue initialMailSyncQueue(
            InitialMailSyncRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue initialMailSyncDeadLetterQueue(InitialMailSyncRabbitProperties properties) {
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
    public MessageRecoverer initialMailSyncMessageRecoverer(
            InitialMailSyncRabbitProperties properties,
            DiscordAlertService discordAlertService
    ) {
        return (message, cause) -> {
            log.warn(
                    "Initial mail sync retries exhausted. Sending to DLQ routingKey={} messageId={} payloadSize={}B",
                    properties.getDeadLetterRoutingKey(),
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            discordAlertService.sendDlqAlert(message, cause);
            throw new AmqpRejectAndDontRequeueException("Initial mail sync retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor rabbitRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("initialMailSyncMessageRecoverer") MessageRecoverer initialMailSyncMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .retryPolicy(RabbitMqConfig.createRetryPolicy())
                .backOffOptions(properties.getRetryInitialInterval(), properties.getRetryMultiplier(), properties.getRetryMaxInterval())
                .recoverer(initialMailSyncMessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("rabbitRetryInterceptor") MethodInterceptor rabbitRetryInterceptor,
            InitialMailSyncRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setPrefetchCount(properties.getPrefetch());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(rabbitRetryInterceptor);
        return factory;
    }
}
