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
    private long retryInitialInterval = 30_000L;
    private double retryMultiplier = 2.0;
    private long retryMaxInterval = 300_000L;
    private Boolean publisherMandatory;
}
