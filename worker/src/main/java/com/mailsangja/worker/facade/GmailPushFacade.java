package com.mailsangja.worker.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryListResult;
import com.mailsangja.worker.dto.gmail.push.GoogleMailPushNotificationResult;
import com.mailsangja.worker.dto.gmail.push.GooglePubsubMessageRequest;
import com.mailsangja.worker.dto.gmail.push.GooglePubsubPushRequest;
import com.mailsangja.worker.handler.mail.GmailHistoryEventClassifier;
import com.mailsangja.worker.handler.mail.GmailHistoryEventHandler;
import com.mailsangja.worker.service.google.GooglePubsubOidcQueryService;
import com.mailsangja.worker.service.google.GoogleMailHistoryQueryService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.MailAccountCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailPushFacade {

    private final GooglePubsubOidcQueryService googlePubsubOidcQueryService;
    private final MailAccountCommandService mailAccountCommandService;
    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    private final GoogleMailHistoryQueryService googleMailHistoryQueryService;
    private final GmailHistoryEventClassifier gmailHistoryEventClassifier;
    private final List<GmailHistoryEventHandler> gmailHistoryEventHandlers;
    private final ObjectMapper objectMapper;

    public void handlePush(String authorizationHeader, GooglePubsubPushRequest request) {
        googlePubsubOidcQueryService.validateAuthorization(authorizationHeader);
        validatePushRequest(request);

        GoogleMailPushNotificationResult notification = decodeNotification(request.message());
        validateNotification(notification);

        MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
                mailAccountQueryService.findActiveGoogleMailAccountByEmailAddress(notification.emailAddress())
        );
        String startHistoryId = resolveStartHistoryId(mailAccount, notification.historyId());

        GoogleMailHistoryListResult historyResult = googleMailHistoryQueryService.getHistory(
                mailAccount.getAccessToken(),
                startHistoryId
        );
        List<GmailHistoryEvent> historyEvents = gmailHistoryEventClassifier.classify(mailAccount, historyResult);

        historyEvents.forEach(this::handleHistoryEvent);

        mailAccountCommandService.updateSyncHistoryId(mailAccount, historyResult.historyId());

        log.info(
                "Processed Gmail push event for emailAddress={} eventHistoryId={} syncedHistoryId={} parsedEventCount={}",
                notification.emailAddress(),
                notification.historyId(),
                historyResult.historyId(),
                historyEvents.size()
        );
    }

    private void handleHistoryEvent(GmailHistoryEvent historyEvent) {
        gmailHistoryEventHandlers.stream()
                .filter(handler -> handler.supports() == historyEvent.eventType())
                .findFirst()
                .orElseThrow(() -> new MailPushException(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID))
                .handle(historyEvent);
    }

    private void validatePushRequest(GooglePubsubPushRequest request) {
        if (request == null || request.message() == null) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_PUSH_REQUEST);
        }
    }

    private GoogleMailPushNotificationResult decodeNotification(GooglePubsubMessageRequest message) {
        if (message == null || isBlank(message.data())) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_MESSAGE_DATA);
        }

        try {
            byte[] decodedData = Base64.getDecoder().decode(message.data());
            return objectMapper.readValue(decodedData, GoogleMailPushNotificationResult.class);
        } catch (IllegalArgumentException | IOException e) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_MESSAGE_DATA);
        }
    }

    private void validateNotification(GoogleMailPushNotificationResult notification) {
        if (notification == null
                || isBlank(notification.emailAddress())
                || isBlank(notification.historyId())) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
        }
    }

    private String resolveStartHistoryId(MailAccount mailAccount, String eventHistoryId) {
        if (!isBlank(mailAccount.getSyncHistoryId())) {
            return mailAccount.getSyncHistoryId();
        }

        return eventHistoryId;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
