package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.GmailHistoryEventRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
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
public class GmailMessagePermanentlyDeletedRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(GmailMessagePermanentlyDeletedRabbitConfig.class);
    private static final GmailHistoryEventType EVENT_TYPE = GmailHistoryEventType.MESSAGE_PERMANENTLY_DELETED;

    @Bean
    public Queue gmailMessagePermanentlyDeletedQueue(
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
    public Queue gmailMessagePermanentlyDeletedDeadLetterQueue(GmailHistoryEventRabbitProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueueName(EVENT_TYPE)).build();
    }

    @Bean
    public Binding gmailMessagePermanentlyDeletedBinding(
            @Qualifier("gmailMessagePermanentlyDeletedQueue") Queue gmailMessagePermanentlyDeletedQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            GmailHistoryEventRabbitProperties properties
    ) {
        return BindingBuilder.bind(gmailMessagePermanentlyDeletedQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey(EVENT_TYPE));
    }

    @Bean
    public Binding gmailMessagePermanentlyDeletedDeadLetterBinding(
            @Qualifier("gmailMessagePermanentlyDeletedDeadLetterQueue") Queue gmailMessagePermanentlyDeletedDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            GmailHistoryEventRabbitProperties properties
    ) {
        return BindingBuilder.bind(gmailMessagePermanentlyDeletedDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey(EVENT_TYPE));
    }

    // 5개 state-change 큐가 공유하는 ContainerFactory
    @Bean
    public MessageRecoverer gmailHistoryStateMessageRecoverer() {
        return (message, cause) -> {
            log.warn(
                    "Gmail history state event retries exhausted. messageId={} payloadSize={}B",
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            throw new AmqpRejectAndDontRequeueException("Gmail history state event retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor gmailHistoryStateRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("gmailHistoryStateMessageRecoverer") MessageRecoverer gmailHistoryStateMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(properties.getRetryMaxAttempts())
                .recoverer(gmailHistoryStateMessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory gmailHistoryStateContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("gmailHistoryStateRetryInterceptor") MethodInterceptor gmailHistoryStateRetryInterceptor,
            MailTaskRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(gmailHistoryStateRetryInterceptor);
        return factory;
    }
}
