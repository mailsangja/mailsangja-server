package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.MailEmbeddingRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
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
public class MailEmbeddingRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(MailEmbeddingRabbitConfig.class);

    @Bean
    public Queue mailEmbeddingQueue(
            MailEmbeddingRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue mailEmbeddingDeadLetterQueue(MailEmbeddingRabbitProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueueName()).build();
    }

    @Bean
    public Binding mailEmbeddingBinding(
            @Qualifier("mailEmbeddingQueue") Queue mailEmbeddingQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            MailEmbeddingRabbitProperties properties
    ) {
        return BindingBuilder.bind(mailEmbeddingQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding mailEmbeddingDeadLetterBinding(
            @Qualifier("mailEmbeddingDeadLetterQueue") Queue mailEmbeddingDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            MailEmbeddingRabbitProperties properties
    ) {
        return BindingBuilder.bind(mailEmbeddingDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageRecoverer mailEmbeddingMessageRecoverer(MailEmbeddingRabbitProperties properties) {
        return (message, cause) -> {
            log.warn(
                    "Mail embedding retries exhausted. Sending to DLQ routingKey={} messageId={} payloadSize={}B",
                    properties.getDeadLetterRoutingKey(),
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            throw new AmqpRejectAndDontRequeueException("Mail embedding retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor mailEmbeddingRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("mailEmbeddingMessageRecoverer") MessageRecoverer mailEmbeddingMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .retryPolicy(RabbitMqConfig.createRetryPolicy(properties.getRetryMaxAttempts()))
                .recoverer(mailEmbeddingMessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory mailEmbeddingRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("mailEmbeddingRetryInterceptor") MethodInterceptor mailEmbeddingRetryInterceptor,
            MailTaskRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(mailEmbeddingRetryInterceptor);
        return factory;
    }
}
