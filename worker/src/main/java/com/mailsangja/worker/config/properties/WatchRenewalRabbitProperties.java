package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.rabbitmq.watch-renewal")
public class WatchRenewalRabbitProperties {

    private String exchange;
    private String queue;
    private String routingKey;
    private String deadLetterExchange;
    private String deadLetterQueue;
    private String deadLetterRoutingKey;
    private Duration ttl;
    private Integer retryMaxAttempts;
    private Integer concurrency;
    private Boolean publisherMandatory;
}
