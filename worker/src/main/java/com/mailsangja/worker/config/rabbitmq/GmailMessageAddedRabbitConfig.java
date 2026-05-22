package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.GmailHistoryEventRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.service.notification.DiscordAlertService;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Configuration
public class GmailMessageAddedRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(GmailMessageAddedRabbitConfig.class);
    private static final GmailHistoryEventType EVENT_TYPE = GmailHistoryEventType.MESSAGE_ADDED;

    @Bean
    public Queue gmailMessageAddedQueue(
            GmailHistoryEventRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        return QueueBuilder.durable(properties.getQueueName(EVENT_TYPE))
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey(EVENT_TYPE))
                .build();
    }

    @Bean
    public Queue gmailMessageAddedDeadLetterQueue(GmailHistoryEventRabbitProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueueName(EVENT_TYPE)).build();
    }

    @Bean
    public Binding gmailMessageAddedBinding(
            @Qualifier("gmailMessageAddedQueue") Queue gmailMessageAddedQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            GmailHistoryEventRabbitProperties properties
    ) {
        return BindingBuilder.bind(gmailMessageAddedQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey(EVENT_TYPE));
    }

    @Bean
    public Binding gmailMessageAddedDeadLetterBinding(
            @Qualifier("gmailMessageAddedDeadLetterQueue") Queue gmailMessageAddedDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            GmailHistoryEventRabbitProperties properties
    ) {
        return BindingBuilder.bind(gmailMessageAddedDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey(EVENT_TYPE));
    }

    @Bean
    public MessageRecoverer gmailMessageAddedMessageRecoverer(
            GmailHistoryEventRabbitProperties properties,
            DiscordAlertService discordAlertService
    ) {
        return (message, cause) -> {
            log.warn(
                    "Gmail message-added retries exhausted. routingKey={} messageId={} payloadSize={}B",
                    properties.getDeadLetterRoutingKey(EVENT_TYPE),
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            discordAlertService.sendDlqAlert(message, cause);
            throw new AmqpRejectAndDontRequeueException("Gmail message-added retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor gmailMessageAddedRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("gmailMessageAddedMessageRecoverer") MessageRecoverer gmailMessageAddedMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .retryPolicy(RabbitMqConfig.createRetryPolicy())
                .backOffOptions(properties.getRetryInitialInterval(), properties.getRetryMultiplier(), properties.getRetryMaxInterval())
                .recoverer(gmailMessageAddedMessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory gmailMessageAddedContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("gmailMessageAddedRetryInterceptor") MethodInterceptor gmailMessageAddedRetryInterceptor,
            GmailHistoryEventRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getMessageAddedConcurrency());
        factory.setMaxConcurrentConsumers(properties.getMessageAddedConcurrency());
        factory.setPrefetchCount(properties.getMessageAddedPrefetch());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(gmailMessageAddedRetryInterceptor);
        return factory;
    }
}
