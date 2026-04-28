package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.handler.mail.GmailHistoryEventHandler;
import com.mailsangja.worker.handler.mail.MessageAddedHistoryEventHandler;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GmailHistoryEventListener {

    private final MessageAddedHistoryEventHandler messageAddedHandler;
    private final List<GmailHistoryEventHandler> stateChangeHandlers;
    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;

    @RabbitListener(
            queues = "#{@gmailMessageAddedQueue.name}",
            containerFactory = "gmailMessageAddedContainerFactory"
    )
    public void handleMessageAdded(GmailHistoryEvent event) {
        MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
                mailAccountQueryService.findActiveMailAccountById(event.mailAccountId())
        );
        messageAddedHandler.handle(mailAccount, event);
    }

    @RabbitListener(
            queues = {
                "#{@gmailMessageReadQueue.name}",
                "#{@gmailMessageUnreadQueue.name}",
                "#{@gmailMessageTrashedQueue.name}",
                "#{@gmailMessageRestoredQueue.name}",
                "#{@gmailMessagePermanentlyDeletedQueue.name}"
            },
            containerFactory = "gmailHistoryStateContainerFactory"
    )
    public void handleStateChange(GmailHistoryEvent event) {
        stateChangeHandlers.stream()
                .filter(h -> h.supports() == event.eventType())
                .findFirst()
                .orElseThrow(() -> new MailPushException(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID))
                .handle(event);
    }
}
