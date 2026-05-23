package com.mailsangja.worker.config.properties;

import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.rabbitmq.gmail-history")
public class GmailHistoryEventRabbitProperties {

    private int messageAddedConcurrency = 2;
    private int messageAddedPrefetch = 3;
    private int stateConcurrency = 3;
    private int statePrefetch = 10;

    public String getQueueName(GmailHistoryEventType eventType) {
        return "mailsangja." + eventType.getTaskName();
    }

    public String getRoutingKey(GmailHistoryEventType eventType) {
        return "mail." + eventType.getTaskName();
    }

    public String getDeadLetterQueueName(GmailHistoryEventType eventType) {
        return getQueueName(eventType) + ".dlq";
    }

    public String getDeadLetterRoutingKey(GmailHistoryEventType eventType) {
        return getRoutingKey(eventType) + ".dlq";
    }
}
