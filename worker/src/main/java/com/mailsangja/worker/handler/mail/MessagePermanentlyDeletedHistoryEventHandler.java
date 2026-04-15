package com.mailsangja.worker.handler.mail;

import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.service.trash.GmailHistoryDeleteCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagePermanentlyDeletedHistoryEventHandler implements GmailHistoryEventHandler {

    private final GmailHistoryDeleteCommandService gmailHistoryDeleteCommandService;

    @Override
    public GmailHistoryEventType supports() {
        return GmailHistoryEventType.MESSAGE_PERMANENTLY_DELETED;
    }

    @Override
    public void handle(GmailHistoryEvent event) {
        gmailHistoryDeleteCommandService.permanentlyDeleteMessage(event);
    }
}
