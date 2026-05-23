package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.rabbitmq.mail-embedding")
public class MailEmbeddingRabbitProperties {

    private static final String TASK_NAME = "embedding";

    private int concurrency = 2;
    private int prefetch = 2;

    public String getTaskName() {
        return TASK_NAME;
    }

    public String getQueueName() {
        return "mailsangja." + TASK_NAME;
    }

    public String getRoutingKey() {
        return "mail." + TASK_NAME;
    }

    public String getDeadLetterQueueName() {
        return getQueueName() + ".dlq";
    }

    public String getDeadLetterRoutingKey() {
        return getRoutingKey() + ".dlq";
    }
}
