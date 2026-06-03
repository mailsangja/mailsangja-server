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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
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
    public void handleMessageAdded(GmailHistoryEvent event, Message rawMessage) {
        handleMessageAdded(event);
    }

    public void handleMessageAdded(GmailHistoryEvent event) {
        log.debug(
                "Gmail message added event handling started. mailAccountId={} gmailThreadId={} gmailMessageId={} historyId={} eventType={}",
                event.mailAccountId(),
                event.gmailThreadId(),
                event.gmailMessageId(),
                event.historyId(),
                event.eventType()
        );
        MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
                mailAccountQueryService.findSyncableMailAccountById(event.mailAccountId())
        );
        messageAddedHandler.handle(mailAccount, event);
        log.debug(
                "Gmail message added event handling completed. mailAccountId={} gmailThreadId={} gmailMessageId={} historyId={} eventType={}",
                event.mailAccountId(),
                event.gmailThreadId(),
                event.gmailMessageId(),
                event.historyId(),
                event.eventType()
        );
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
    public void handleStateChange(GmailHistoryEvent event, Message rawMessage) {
        handleStateChange(event);
    }

    public void handleStateChange(GmailHistoryEvent event) {
        log.debug(
                "Gmail state change event handling started. mailAccountId={} gmailThreadId={} gmailMessageId={} historyId={} eventType={}",
                event.mailAccountId(),
                event.gmailThreadId(),
                event.gmailMessageId(),
                event.historyId(),
                event.eventType()
        );
        GmailHistoryEventHandler handler = stateChangeHandlers.stream()
                .filter(h -> h.supports() == event.eventType())
                .findFirst()
                .orElse(null);
        if (handler == null) {
            log.warn(
                    "Gmail state change event handler not found. mailAccountId={} gmailThreadId={} gmailMessageId={} historyId={} eventType={}",
                    event.mailAccountId(),
                    event.gmailThreadId(),
                    event.gmailMessageId(),
                    event.historyId(),
                    event.eventType()
            );
            throw new MailPushException(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID);
        }

        handler.handle(event);
        log.debug(
                "Gmail state change event handling completed. mailAccountId={} gmailThreadId={} gmailMessageId={} historyId={} eventType={}",
                event.mailAccountId(),
                event.gmailThreadId(),
                event.gmailMessageId(),
                event.historyId(),
                event.eventType()
        );
    }
}
