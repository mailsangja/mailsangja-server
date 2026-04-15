package com.mailsangja.worker.handler.mail;

import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;

public interface GmailHistoryEventHandler {

    GmailHistoryEventType supports();

    void handle(GmailHistoryEvent event);
}
