package com.mailsangja.worker.handler.mail;

import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.service.mail.GmailHistoryStateCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageUnreadHistoryEventHandler implements GmailHistoryEventHandler {

    private final GmailHistoryStateCommandService gmailHistoryStateCommandService;

    @Override
    public GmailHistoryEventType supports() {
        return GmailHistoryEventType.MESSAGE_UNREAD;
    }

    @Override
    public void handle(GmailHistoryEvent event) {
        gmailHistoryStateCommandService.markMessageAsUnread(event);
    }
}
