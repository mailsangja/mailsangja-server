package com.mailsangja.worker.config.rabbitmq;

import com.mailsangja.worker.common.exception.mail.MailAccountNotFoundException;
import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;
import com.mailsangja.worker.common.observability.ObservabilitySupport;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RabbitMqConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfig.class);
    private static final long MAX_QUEUE_TTL_MILLIS = Integer.MAX_VALUE;
    static final int MAX_RETRIES = 3;

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

    @Bean("publisherConnectionFactory")
    public CachingConnectionFactory publisherConnectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password,
            @Value("${spring.rabbitmq.virtual-host:/}") String virtualHost,
            @Value("${spring.rabbitmq.publisher-returns:false}") boolean publisherReturns
    ) {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setPublisherReturns(publisherReturns);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            @Qualifier("publisherConnectionFactory") ConnectionFactory publisherConnectionFactory,
            MessageConverter rabbitMessageConverter,
            MailTaskRabbitProperties properties,
            ObservabilitySupport observabilitySupport
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(publisherConnectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        rabbitTemplate.setBeforePublishPostProcessors(observabilitySupport.rabbitHeaders());
        rabbitTemplate.setObservationEnabled(true);
        boolean mandatory = Boolean.TRUE.equals(properties.getPublisherMandatory());
        rabbitTemplate.setMandatory(mandatory);
        if (mandatory) {
            rabbitTemplate.setReturnsCallback(returned ->
                    log.warn("[MQ] Message returned: exchange={}, routingKey={}, replyCode={}, replyText={}",
                            returned.getExchange(), returned.getRoutingKey(),
                            returned.getReplyCode(), returned.getReplyText())
            );
        }
        return rabbitTemplate;
    }

    @Bean
    public BeanPostProcessor rabbitListenerContainerFactoryObservationPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof SimpleRabbitListenerContainerFactory factory) {
                    factory.setObservationEnabled(true);
                }
                return bean;
            }
        };
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

    static MethodInterceptor createRetryInterceptor(MailTaskRabbitProperties properties, MessageRecoverer recoverer) {
        return RetryInterceptorBuilder.stateless()
                .configureRetryPolicy(policy -> policy
                        .maxRetries(MAX_RETRIES)
                        .excludes(MailAccountNotFoundException.class)
                        .delay(Duration.ofMillis(properties.getRetryInitialInterval()))
                        .multiplier(properties.getRetryMultiplier())
                        .maxDelay(Duration.ofMillis(properties.getRetryMaxInterval())))
                .recoverer(recoverer)
                .build();
    }

    static void validateTaskName(String taskName, String propertyName) {
        if (taskName == null || taskName.isBlank()) {
            throw new MqException(MqErrorCode.INVALID_RABBITMQ_TASK_NAME, propertyName + " must not be blank.");
        }
    }
}
