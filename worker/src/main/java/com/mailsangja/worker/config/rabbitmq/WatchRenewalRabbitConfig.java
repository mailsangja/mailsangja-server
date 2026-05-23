package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.WatchRenewalRabbitProperties;
import com.mailsangja.worker.service.notification.DiscordAlertService;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

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
public class WatchRenewalRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(WatchRenewalRabbitConfig.class);

    @Bean
    public Queue watchRenewalQueue(
            WatchRenewalRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue watchRenewalDeadLetterQueue(WatchRenewalRabbitProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueueName()).build();
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
    public MessageRecoverer watchRenewalMessageRecoverer(
            WatchRenewalRabbitProperties properties,
            DiscordAlertService discordAlertService
    ) {
        return (message, cause) -> {
            log.warn(
                    "Watch renewal retries exhausted. Sending to DLQ routingKey={} messageId={} payloadSize={}B",
                    properties.getDeadLetterRoutingKey(),
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            discordAlertService.sendDlqAlert(message, cause);
            throw new AmqpRejectAndDontRequeueException("Watch renewal retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor watchRenewalRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("watchRenewalMessageRecoverer") MessageRecoverer watchRenewalMessageRecoverer
    ) {
        return RabbitMqConfig.createRetryInterceptor(properties, watchRenewalMessageRecoverer);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory watchRenewalRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("watchRenewalRetryInterceptor") MethodInterceptor watchRenewalRetryInterceptor,
            WatchRenewalRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setPrefetchCount(properties.getPrefetch());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(watchRenewalRetryInterceptor);
        return factory;
    }
}
