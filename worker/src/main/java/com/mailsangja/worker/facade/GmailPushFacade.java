package com.mailsangja.worker.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryListResult;
import com.mailsangja.worker.dto.gmail.push.GoogleMailPushNotificationResult;
import com.mailsangja.worker.dto.gmail.push.GooglePubsubPushRequest;
import com.mailsangja.worker.handler.mail.GmailHistoryEventClassifier;
import com.mailsangja.worker.messaging.publisher.GmailHistoryEventPublisher;
import com.mailsangja.worker.service.google.GmailHistoryApiService;
import com.mailsangja.worker.service.google.GooglePubsubOidcApiService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.MailAccountCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailPushFacade {

    private final GooglePubsubOidcApiService googlePubsubOidcApiService;
    private final MailAccountCommandService mailAccountCommandService;
    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    private final GmailHistoryApiService gmailHistoryApiService;
    private final GmailHistoryEventClassifier gmailHistoryEventClassifier;
    private final GmailHistoryEventPublisher gmailHistoryEventPublisher;
    private final ObjectMapper objectMapper;

    public void handlePush(String authorizationHeader, GooglePubsubPushRequest request) {
        googlePubsubOidcApiService.validateAuthorization(authorizationHeader);

        GoogleMailPushNotificationResult notification = request.decode(objectMapper);

        MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
                mailAccountQueryService.findActiveGoogleMailAccountByEmailAddress(notification.emailAddress())
        );

        GoogleMailHistoryListResult historyResult = gmailHistoryApiService.getHistory(
                mailAccount.getAccessToken(),
                mailAccount.resolveStartHistoryId(notification.historyId())
        );

        List<GmailHistoryEvent> historyEvents = gmailHistoryEventClassifier.classify(mailAccount, historyResult);

        gmailHistoryEventPublisher.publishAll(historyEvents);

        mailAccountCommandService.updateSyncHistoryId(mailAccount, historyResult.historyId());

        log.info(
                "Processed Gmail push event. emailAddress={} eventHistoryId={} syncedHistoryId={} publishedEventCount={}",
                notification.emailAddress(),
                notification.historyId(),
                historyResult.historyId(),
                historyEvents.size()
        );
    }
}
