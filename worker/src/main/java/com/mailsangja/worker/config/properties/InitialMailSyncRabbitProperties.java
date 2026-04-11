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

    private String taskName;
    private String threadBatchTaskName;

    public String getQueueName() {
        return "mailsangja." + taskName;
    }

    public String getRoutingKey() {
        return "mail." + taskName;
    }

    public String getDeadLetterQueueName() {
        return getQueueName() + ".dlq";
    }

    public String getDeadLetterRoutingKey() {
        return getRoutingKey() + ".dlq";
    }

    public String getThreadBatchQueueName() {
        return "mailsangja." + threadBatchTaskName;
    }

    public String getThreadBatchRoutingKey() {
        return "mail." + threadBatchTaskName;
    }

    public String getThreadBatchDeadLetterQueueName() {
        return getThreadBatchQueueName() + ".dlq";
    }

    public String getThreadBatchDeadLetterRoutingKey() {
        return getThreadBatchRoutingKey() + ".dlq";
    }
}
