package com.mailsangja.worker.config.properties;

import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import org.springframework.stereotype.Component;

@Component
public class GmailHistoryEventRabbitProperties {

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
