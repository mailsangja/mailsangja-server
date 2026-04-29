package com.mailsangja.worker.config.properties;

import org.springframework.stereotype.Component;

@Component
public class InitialMailSyncRabbitProperties {

    private static final String TASK_NAME = "sync.gmail.initial";
    private static final String THREAD_BATCH_TASK_NAME = "sync.gmail.initial.thread-batch";

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

    public String getThreadBatchTaskName() {
        return THREAD_BATCH_TASK_NAME;
    }

    public String getThreadBatchQueueName() {
        return "mailsangja." + THREAD_BATCH_TASK_NAME;
    }

    public String getThreadBatchRoutingKey() {
        return "mail." + THREAD_BATCH_TASK_NAME;
    }

    public String getThreadBatchDeadLetterQueueName() {
        return getThreadBatchQueueName() + ".dlq";
    }

    public String getThreadBatchDeadLetterRoutingKey() {
        return getThreadBatchRoutingKey() + ".dlq";
    }
}
