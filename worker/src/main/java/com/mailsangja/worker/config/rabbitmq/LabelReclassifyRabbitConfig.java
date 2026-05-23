package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.LabelReclassifyRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.service.notification.DiscordAlertService;
import lombok.extern.slf4j.Slf4j;
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

@Configuration
@Slf4j
public class LabelReclassifyRabbitConfig {

    @Bean
    public Queue labelReclassifyQueue(
            LabelReclassifyRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        RabbitMqConfig.validateTaskName(properties.getTaskName(), "mailsangja.rabbitmq.label-reclassify.task-name");
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue labelReclassifyDeadLetterQueue(LabelReclassifyRabbitProperties properties) {
        RabbitMqConfig.validateTaskName(properties.getTaskName(), "mailsangja.rabbitmq.label-reclassify.task-name");
        return QueueBuilder.durable(properties.getDeadLetterQueueName()).build();
    }

    @Bean
    public Binding labelReclassifyBinding(
            @Qualifier("labelReclassifyQueue") Queue labelReclassifyQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            LabelReclassifyRabbitProperties properties
    ) {
        return BindingBuilder.bind(labelReclassifyQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding labelReclassifyDeadLetterBinding(
            @Qualifier("labelReclassifyDeadLetterQueue") Queue labelReclassifyDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            LabelReclassifyRabbitProperties properties
    ) {
        return BindingBuilder.bind(labelReclassifyDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageRecoverer labelReclassifyMessageRecoverer(
            LabelReclassifyRabbitProperties properties,
            DiscordAlertService discordAlertService
    ) {
        return (message, cause) -> {
            log.warn(
                    "LabelReclassify retries exhausted. Sending to DLQ routingKey={} messageId={} payloadSize={}B",
                    properties.getDeadLetterRoutingKey(),
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            discordAlertService.sendDlqAlert(message, cause);
            throw new AmqpRejectAndDontRequeueException("LabelReclassify retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor labelReclassifyRetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("labelReclassifyMessageRecoverer") MessageRecoverer labelReclassifyMessageRecoverer
    ) {
        return RabbitMqConfig.createRetryInterceptor(properties, labelReclassifyMessageRecoverer);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory labelReclassifyRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("labelReclassifyRetryInterceptor") MethodInterceptor labelReclassifyRetryInterceptor,
            LabelReclassifyRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setPrefetchCount(properties.getPrefetch());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(labelReclassifyRetryInterceptor);
        return factory;
    }
}
