package com.mailsangja.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.rabbitmq.label-reclassify")
public class LabelReclassifyRabbitProperties {

    private String taskName;

    public String getTaskName() {
        return taskName;
    }

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
}
