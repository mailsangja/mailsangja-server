package com.mailsangja.worker.service.mail;

import com.mailsangja.worker.dto.gmail.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.GmailHistoryEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageUnreadHistoryEventHandler implements GmailHistoryEventHandler {

    @Override
    public GmailHistoryEventType supports() {
        return GmailHistoryEventType.MESSAGE_UNREAD;
    }

    @Override
    public void handle(GmailHistoryEvent event) {
        log.debug(
                "Deferred Gmail message unread event handling mailAccountId={} gmailThreadId={} gmailMessageId={} historyId={}",
                event.mailAccountId(),
                event.gmailThreadId(),
                event.gmailMessageId(),
                event.historyId()
        );
    }
}
