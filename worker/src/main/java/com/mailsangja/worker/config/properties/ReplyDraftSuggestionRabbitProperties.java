package com.mailsangja.worker.config.properties;

import org.springframework.stereotype.Component;

@Component
public class ReplyDraftSuggestionRabbitProperties {

    private static final String TASK_NAME = "suggest.reply-draft.gmail";

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
