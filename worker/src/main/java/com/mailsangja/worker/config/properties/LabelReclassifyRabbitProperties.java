package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.rabbitmq.label-reclassify")
public class LabelReclassifyRabbitProperties {

    private static final String TASK_NAME = "label.reclassify";

    private int messageBatchSize = 200;
    private int threadBatchSize = 50;

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
