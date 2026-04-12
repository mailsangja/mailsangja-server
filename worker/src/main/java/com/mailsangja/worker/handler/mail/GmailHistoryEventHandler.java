package com.mailsangja.worker.handler.mail;

import com.mailsangja.worker.dto.gmail.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.GmailHistoryEventType;

public interface GmailHistoryEventHandler {

    GmailHistoryEventType supports();

    void handle(GmailHistoryEvent event);
}
