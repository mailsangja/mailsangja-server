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
public class GmailMessageUnreadRabbitConfig {

    private static final GmailHistoryEventType EVENT_TYPE = GmailHistoryEventType.MESSAGE_UNREAD;

    @Bean
    public Queue gmailMessageUnreadQueue(
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
    public Queue gmailMessageUnreadDeadLetterQueue(GmailHistoryEventRabbitProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueueName(EVENT_TYPE)).build();
    }

    @Bean
    public Binding gmailMessageUnreadBinding(
            @Qualifier("gmailMessageUnreadQueue") Queue gmailMessageUnreadQueue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            GmailHistoryEventRabbitProperties properties
    ) {
        return BindingBuilder.bind(gmailMessageUnreadQueue)
                .to(mailTaskExchange)
                .with(properties.getRoutingKey(EVENT_TYPE));
    }

    @Bean
    public Binding gmailMessageUnreadDeadLetterBinding(
            @Qualifier("gmailMessageUnreadDeadLetterQueue") Queue gmailMessageUnreadDeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            GmailHistoryEventRabbitProperties properties
    ) {
        return BindingBuilder.bind(gmailMessageUnreadDeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey(EVENT_TYPE));
    }
}
