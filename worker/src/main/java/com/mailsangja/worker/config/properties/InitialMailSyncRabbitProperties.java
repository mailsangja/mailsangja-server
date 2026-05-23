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

    private static final String TASK_NAME = "sync.gmail.initial";
    private static final String THREAD_BATCH_TASK_NAME = "sync.gmail.initial.thread-batch";

    private int concurrency = 1;
    private int prefetch = 1;
    private int threadBatchConcurrency = 3;
    private int threadBatchPrefetch = 5;

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
