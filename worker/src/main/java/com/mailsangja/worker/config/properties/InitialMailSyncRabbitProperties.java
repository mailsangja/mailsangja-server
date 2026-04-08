package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.rabbitmq.initial-mail-sync")
public class InitialMailSyncRabbitProperties {

    private String exchange;
    private String queue;
    private String routingKey;
}
