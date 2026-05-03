package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.PaymentWebhookRabbitProperties;
import lombok.extern.slf4j.Slf4j;
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

@Configuration
@Slf4j
public class PaymentRabbitConfig {

    @Bean
    public Queue paymentWebhookQueue(
            PaymentWebhookRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        RabbitMqConfig.validateTaskName(properties.getTaskName(), "mailsangja.rabbitmq.payment-webhook");
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue paymentWebhookDeadLetterQueue(PaymentWebhookRabbitProperties properties) {
        RabbitMqConfig.validateTaskName(properties.getTaskName(), "mailsangja.rabbitmq.payment-webhook");
        return QueueBuilder.durable(properties.getDeadLetterQueueName()).build();
    }

    @Bean
    public Binding paymentWebhookBinding(
            @Qualifier("paymentWebhookQueue") Queue paymentWebhookQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            PaymentWebhookRabbitProperties properties
    ) {
        return BindingBuilder.bind(paymentWebhookQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding paymentWebhookDeadLetterBinding(
            @Qualifier("paymentWebhookDeadLetterQueue") Queue paymentWebhookDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            PaymentWebhookRabbitProperties properties
    ) {
        return BindingBuilder.bind(paymentWebhookDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageRecoverer paymentWebhookMessageRecoverer(PaymentWebhookRabbitProperties properties) {
        return (message, cause) -> {
            log.warn(
                    "PaymentWebhook retries exhausted. Sending to DLQ routingKey={} messageId={} payloadSize={}B",
                    properties.getDeadLetterRoutingKey(),
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            throw new AmqpRejectAndDontRequeueException("PaymentWebhook retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor paymentWebhookRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("paymentWebhookMessageRecoverer") MessageRecoverer paymentWebhookMessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(properties.getRetryMaxAttempts())
                .recoverer(paymentWebhookMessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory paymentWebhookRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("paymentWebhookRetryInterceptor") MethodInterceptor paymentWebhookRetryInterceptor,
            MailTaskRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(paymentWebhookRetryInterceptor);
        return factory;
    }
}
