package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.GmailHistoryEventRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GmailMessageRestoredRabbitConfig {

    private static final GmailHistoryEventType EVENT_TYPE = GmailHistoryEventType.MESSAGE_RESTORED;

    @Bean
    public Queue gmailMessageRestoredQueue(
            GmailHistoryEventRabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        RabbitMqConfig.validateTaskName(properties.getTaskName(EVENT_TYPE), "mailsangja.rabbitmq.gmail-history-event.message-restored-task-name");
        return QueueBuilder.durable(properties.getQueueName(EVENT_TYPE))
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey(EVENT_TYPE))
                .build();
    }

    @Bean
    public Queue gmailMessageRestoredDeadLetterQueue(GmailHistoryEventRabbitProperties properties) {
        RabbitMqConfig.validateTaskName(properties.getTaskName(EVENT_TYPE), "mailsangja.rabbitmq.gmail-history-event.message-restored-task-name");
        return QueueBuilder.durable(properties.getDeadLetterQueueName(EVENT_TYPE)).build();
    }

    @Bean
    public Binding gmailMessageRestoredBinding(
            @Qualifier("gmailMessageRestoredQueue") Queue gmailMessageRestoredQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            GmailHistoryEventRabbitProperties properties
    ) {
        return BindingBuilder.bind(gmailMessageRestoredQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey(EVENT_TYPE));
    }

    @Bean
    public Binding gmailMessageRestoredDeadLetterBinding(
            @Qualifier("gmailMessageRestoredDeadLetterQueue") Queue gmailMessageRestoredDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            GmailHistoryEventRabbitProperties properties
    ) {
        return BindingBuilder.bind(gmailMessageRestoredDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey(EVENT_TYPE));
    }
}
