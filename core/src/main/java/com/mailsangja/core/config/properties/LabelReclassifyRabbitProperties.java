package com.mailsangja.core.config.properties;

import org.springframework.stereotype.Component;

@Component
public class LabelReclassifyRabbitProperties {

    private static final String TASK_NAME = "label.reclassify";

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
