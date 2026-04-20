package com.mailsangja.worker.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryListResult;
import com.mailsangja.worker.dto.gmail.push.GooglePubsubMessageRequest;
import com.mailsangja.worker.dto.gmail.push.GooglePubsubPushRequest;
import com.mailsangja.worker.handler.mail.GmailHistoryEventClassifier;
import com.mailsangja.worker.handler.mail.GmailHistoryEventHandler;
import com.mailsangja.worker.service.google.GoogleMailHistoryQueryService;
import com.mailsangja.worker.service.google.GooglePubsubOidcQueryService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.MailAccountCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailPushFacade 테스트")
class GmailPushFacadeTest {

    @Mock
    private GooglePubsubOidcQueryService googlePubsubOidcQueryService;

    @Mock
    private MailAccountCommandService mailAccountCommandService;

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;

    @Mock
    private GoogleMailHistoryQueryService googleMailHistoryQueryService;

    @Mock
    private GmailHistoryEventClassifier gmailHistoryEventClassifier;

    @Mock
    private GmailHistoryEventHandler gmailHistoryEventHandler;

    private GmailPushFacade gmailPushFacade;

    @BeforeEach
    void setUp() {
        gmailPushFacade = new GmailPushFacade(
                googlePubsubOidcQueryService,
                mailAccountCommandService,
                mailAccountQueryService,
                googleAccessTokenEnsureService,
                googleMailHistoryQueryService,
                gmailHistoryEventClassifier,
                List.of(gmailHistoryEventHandler),
                new ObjectMapper()
        );
    }

    @Nested
    @DisplayName("handlePush")
    class HandlePush {

        @Test
        @DisplayName("유효한 push 요청이면 history를 조회하고 sync history를 갱신한다")
        void handlePush_유효한Push요청이면History를조회하고SyncHistory를갱신한다() {
            // given
            MailAccount mailAccount = createMailAccount("saved-history");
            GooglePubsubPushRequest request = createPushRequest("user@gmail.com", "event-history");
            GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult("latest-history", List.of());
            GmailHistoryEvent event = new GmailHistoryEvent(
                    GmailHistoryEventType.MESSAGE_ADDED,
                    mailAccount.getId(),
                    "message-1",
                    "thread-1",
                    "history-1"
            );
            given(mailAccountQueryService.findActiveGoogleMailAccountByEmailAddress("user@gmail.com")).willReturn(mailAccount);
            given(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).willReturn(mailAccount);
            given(googleMailHistoryQueryService.getHistory("access-token", "saved-history")).willReturn(historyResult);
            given(gmailHistoryEventClassifier.classify(mailAccount, historyResult)).willReturn(List.of(event));
            given(gmailHistoryEventHandler.supports()).willReturn(GmailHistoryEventType.MESSAGE_ADDED);

            // when
            gmailPushFacade.handlePush("Bearer token", request);

            // then
            then(googlePubsubOidcQueryService).should().validateAuthorization("Bearer token");
            then(googleMailHistoryQueryService).should().getHistory("access-token", "saved-history");
            then(gmailHistoryEventHandler).should().handle(event);
            then(mailAccountCommandService).should().updateSyncHistoryId(mailAccount, "latest-history");
        }

        @Test
        @DisplayName("message가 없으면 invalid push request 예외를 반환한다")
        void handlePush_message가없으면InvalidPushRequest예외를반환한다() {
            // given
            GooglePubsubPushRequest request = new GooglePubsubPushRequest(null, "subscription");

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> gmailPushFacade.handlePush("Bearer token", request)
            );

            // then
            assertEquals("MS-MAIL-INVALID-PUBSUB-PUSH-REQUEST", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("notification의 emailAddress가 비어 있으면 예외를 반환한다")
        void handlePush_notification의EmailAddress가비어있으면예외를반환한다() {
            // given
            GooglePubsubPushRequest request = createPushRequest(" ", "event-history");

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> gmailPushFacade.handlePush("Bearer token", request)
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-PUSH-NOTIFICATION", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("지원하는 handler가 없으면 history result invalid 예외를 반환한다")
        void handlePush_지원하는Handler가없으면HistoryResultInvalid예외를반환한다() {
            // given
            MailAccount mailAccount = createMailAccount(null);
            GooglePubsubPushRequest request = createPushRequest("user@gmail.com", "event-history");
            GmailHistoryEvent event = new GmailHistoryEvent(
                    GmailHistoryEventType.MESSAGE_TRASHED,
                    mailAccount.getId(),
                    "message-1",
                    "thread-1",
                    "history-1"
            );
            GoogleMailHistoryListResult historyResult = new GoogleMailHistoryListResult("latest-history", List.of());
            given(mailAccountQueryService.findActiveGoogleMailAccountByEmailAddress("user@gmail.com")).willReturn(mailAccount);
            given(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).willReturn(mailAccount);
            given(googleMailHistoryQueryService.getHistory("access-token", "event-history")).willReturn(historyResult);
            given(gmailHistoryEventClassifier.classify(mailAccount, historyResult)).willReturn(List.of(event));
            given(gmailHistoryEventHandler.supports()).willReturn(GmailHistoryEventType.MESSAGE_ADDED);

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> gmailPushFacade.handlePush("Bearer token", request)
            );

            // then
            assertEquals("MS-MAIL-GMAIL-HISTORY-RESULT-INVALID", exception.getErrorCode().getCode());
            then(mailAccountCommandService).shouldHaveNoInteractions();
        }
    }

    private GooglePubsubPushRequest createPushRequest(String emailAddress, String historyId) {
        String payload = Base64.getEncoder().encodeToString(
                """
                        {
                          "emailAddress": "%s",
                          "historyId": "%s"
                        }
                        """.formatted(emailAddress, historyId).getBytes(StandardCharsets.UTF_8)
        );
        return new GooglePubsubPushRequest(
                new GooglePubsubMessageRequest(payload, "message-id", "2026-04-20T10:00:00Z", Map.of()),
                "subscription"
        );
    }

    private MailAccount createMailAccount(String syncHistoryId) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@gmail.com")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .syncHistoryId(syncHistoryId)
                .active(true)
                .build();
    }
}
