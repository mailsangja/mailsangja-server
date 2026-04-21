package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

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
}
