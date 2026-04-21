package com.mailsangja.worker.config.properties;

import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.rabbitmq.gmail-history-event")
public class GmailHistoryEventRabbitProperties {

    private String messageAddedTaskName;
    private String messageReadTaskName;
    private String messageUnreadTaskName;
    private String messageTrashedTaskName;
    private String messageRestoredTaskName;
    private String messagePermanentlyDeletedTaskName;

    public String getTaskName(GmailHistoryEventType eventType) {
        return switch (eventType) {
            case MESSAGE_ADDED -> messageAddedTaskName;
            case MESSAGE_READ -> messageReadTaskName;
            case MESSAGE_UNREAD -> messageUnreadTaskName;
            case MESSAGE_TRASHED -> messageTrashedTaskName;
            case MESSAGE_RESTORED -> messageRestoredTaskName;
            case MESSAGE_PERMANENTLY_DELETED -> messagePermanentlyDeletedTaskName;
        };
    }

    public String getQueueName(GmailHistoryEventType eventType) {
        return "mailsangja." + getTaskName(eventType);
    }

    public String getRoutingKey(GmailHistoryEventType eventType) {
        return "mail." + getTaskName(eventType);
    }

    public String getDeadLetterQueueName(GmailHistoryEventType eventType) {
        return getQueueName(eventType) + ".dlq";
    }

    public String getDeadLetterRoutingKey(GmailHistoryEventType eventType) {
        return getRoutingKey(eventType) + ".dlq";
    }
}
