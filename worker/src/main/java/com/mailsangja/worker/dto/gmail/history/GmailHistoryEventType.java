package com.mailsangja.worker.dto.gmail.history;

public enum GmailHistoryEventType {
    MESSAGE_ADDED("event.gmail.message-added"),
    MESSAGE_READ("event.gmail.message-read"),
    MESSAGE_UNREAD("event.gmail.message-unread"),
    MESSAGE_TRASHED("event.gmail.message-trashed"),
    MESSAGE_RESTORED("event.gmail.message-restored"),
    MESSAGE_PERMANENTLY_DELETED("event.gmail.message-permanently-deleted");

    private final String taskName;

    GmailHistoryEventType(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getRoutingKey() {
        return "mail." + taskName;
    }
}
