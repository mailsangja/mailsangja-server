package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.common.exception.mail.MailAccountNotFoundException;
import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;

import java.time.Duration;

@Configuration
public class RabbitMqConfig {

    private static final long MAX_QUEUE_TTL_MILLIS = Integer.MAX_VALUE;

    @Bean
    public DirectExchange mailTaskExchange(MailTaskRabbitProperties properties) {
        return new DirectExchange(properties.getExchange());
    }

    @Bean
    public DirectExchange mailTaskDeadLetterExchange(MailTaskRabbitProperties properties) {
        return new DirectExchange(properties.getDeadLetterExchange());
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            MailTaskRabbitProperties properties
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        rabbitTemplate.setMandatory(Boolean.TRUE.equals(properties.getPublisherMandatory()));
        return rabbitTemplate;
    }

    static int toQueueTtlMillis(Duration ttl, String propertyName) {
        if (ttl == null) {
            throw new MqException(MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL, propertyName + " must not be null.");
        }
        if (ttl.isNegative()) {
            throw new MqException(MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL, propertyName + " must be greater than or equal to 0.");
        }
        long ttlMillis = ttl.toMillis();
        if (ttlMillis > MAX_QUEUE_TTL_MILLIS) {
            throw new MqException(MqErrorCode.INVALID_RABBITMQ_QUEUE_TTL, propertyName + " must be less than or equal to " + MAX_QUEUE_TTL_MILLIS + "ms.");
        }
        return (int) ttlMillis;
    }

    static RetryPolicy createRetryPolicy(int maxRetries) {
        return RetryPolicy.builder()
                .maxRetries(maxRetries)
                .excludes(MailAccountNotFoundException.class)
                .build();
    }

    static void validateTaskName(String taskName, String propertyName) {
        if (taskName == null || taskName.isBlank()) {
            throw new MqException(MqErrorCode.INVALID_RABBITMQ_TASK_NAME, propertyName + " must not be blank.");
        }
    }
}
