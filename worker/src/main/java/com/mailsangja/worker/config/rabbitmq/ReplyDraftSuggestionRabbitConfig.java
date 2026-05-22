package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.ReplyDraftSuggestionRabbitProperties;
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

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReplyDraftSuggestionRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(ReplyDraftSuggestionRabbitConfig.class);

    @Bean
    public Queue replyDraftSuggestionQueue(
            ReplyDraftSuggestionRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue replyDraftSuggestionDeadLetterQueue(ReplyDraftSuggestionRabbitProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueueName()).build();
    }

    @Bean
    public Binding replyDraftSuggestionBinding(
            @Qualifier("replyDraftSuggestionQueue") Queue replyDraftSuggestionQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            ReplyDraftSuggestionRabbitProperties properties
    ) {
        return BindingBuilder.bind(replyDraftSuggestionQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding replyDraftSuggestionDeadLetterBinding(
            @Qualifier("replyDraftSuggestionDeadLetterQueue") Queue replyDraftSuggestionDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            ReplyDraftSuggestionRabbitProperties properties
    ) {
        return BindingBuilder.bind(replyDraftSuggestionDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageRecoverer replyDraftSuggestionMessageRecoverer(
            ReplyDraftSuggestionRabbitProperties properties,
            DiscordAlertService discordAlertService
    ) {
        return (message, cause) -> {
            log.warn(
                    "Reply draft suggestion retries exhausted. Sending to DLQ routingKey={} messageId={} payloadSize={}B",
                    properties.getDeadLetterRoutingKey(),
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            discordAlertService.sendDlqAlert(message, cause);
            throw new AmqpRejectAndDontRequeueException("Reply draft suggestion retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor replyDraftSuggestionRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("replyDraftSuggestionMessageRecoverer") MessageRecoverer replyDraftSuggestionMessageRecoverer
    ) {
        return RabbitMqConfig.createRetryInterceptor(properties, replyDraftSuggestionMessageRecoverer);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory replyDraftSuggestionRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("replyDraftSuggestionRetryInterceptor") MethodInterceptor replyDraftSuggestionRetryInterceptor,
            ReplyDraftSuggestionRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setPrefetchCount(properties.getPrefetch());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(replyDraftSuggestionRetryInterceptor);
        return factory;
    }
}
