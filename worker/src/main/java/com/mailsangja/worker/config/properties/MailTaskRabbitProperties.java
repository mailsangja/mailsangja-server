package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.rabbitmq.task")
public class MailTaskRabbitProperties {

    private String exchange;
    private String deadLetterExchange;
    private Duration ttl;
    private Integer retryMaxAttempts;
    private Integer concurrency;
    private Boolean publisherMandatory;
}
