package com.mailsangja.core.config;

import com.mailsangja.core.config.properties.InitialMailSyncRabbitProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange initialMailSyncExchange(InitialMailSyncRabbitProperties properties) {
        return new DirectExchange(properties.getExchange());
    }

    @Bean
    public Queue initialMailSyncQueue(InitialMailSyncRabbitProperties properties) {
        return new Queue(properties.getQueue(), true);
    }

    @Bean
    public Binding initialMailSyncBinding(
            Queue initialMailSyncQueue,
            DirectExchange initialMailSyncExchange,
            InitialMailSyncRabbitProperties properties
    ) {
        return BindingBuilder.bind(initialMailSyncQueue)
                .to(initialMailSyncExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }
}
