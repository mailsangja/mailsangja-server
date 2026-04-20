package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.worker.dto.mail.sync.NewMessageApplyResult;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import com.mailsangja.worker.service.google.GoogleMailMessageQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailNewMessageSyncCommandService 테스트")
class GmailNewMessageSyncCommandServiceTest {

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;

    @Mock
    private GoogleMailMessageQueryService googleMailMessageQueryService;

    @Mock
    private GmailNewMessageApplyCommandService gmailNewMessageApplyCommandService;

    private GmailNewMessageSyncCommandService service;

    @BeforeEach
    void setUp() {
        service = new GmailNewMessageSyncCommandService(
                mailAccountQueryService,
                googleAccessTokenEnsureService,
                googleMailMessageQueryService,
                gmailNewMessageApplyCommandService
        );
    }

    @Nested
    @DisplayName("syncNewMessage")
    class SyncNewMessage {

        @Test
        @DisplayName("유효한 event면 새 메시지를 동기화하고 push context를 반환한다")
        void syncNewMessage_유효한Event면새메시지를동기화하고PushContext를반환한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(mailAccount.getId());
            InitialMailSyncMessageResult messageResult = createMessageResult("message-1", "subject", "snippet");
            InitialMailSyncThreadResult threadResult = new InitialMailSyncThreadResult("thread-1", "history-1", List.of(messageResult));
            NewMessageApplyResult applyResult = new NewMessageApplyResult(UUID.randomUUID(), UUID.randomUUID());
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).willReturn(mailAccount);
            given(googleMailMessageQueryService.getThreads("access-token", List.of("thread-1"))).willReturn(List.of(threadResult));
            given(gmailNewMessageApplyCommandService.applyNewMessageSync(eqMailAccount(mailAccount), eqEvent(event), anySyncCommand()))
                    .willReturn(applyResult);

            // when
            NewMailPushContext result = service.syncNewMessage(event);

            // then
            assertEquals(mailAccount.getId(), result.mailAccountId());
            assertEquals("alias", result.alias());
            assertEquals("subject", result.subject());
            assertEquals("snippet", result.snippet());
            assertEquals(applyResult.threadId(), result.threadId());
            assertEquals(applyResult.messageId(), result.messageId());
        }

        @Test
        @DisplayName("thread 결과가 비어 있으면 예외를 반환한다")
        void syncNewMessage_thread결과가비어있으면예외를반환한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(mailAccount.getId());
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).willReturn(mailAccount);
            given(googleMailMessageQueryService.getThreads("access-token", List.of("thread-1"))).willReturn(List.of());

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.syncNewMessage(event)
            );

            // then
            assertEquals("MS-MAIL-GMAIL-MESSAGES-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("event가 비어 있으면 예외를 반환한다")
        void syncNewMessage_event가비어있으면예외를반환한다() {
            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.syncNewMessage(null)
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-PUSH-NOTIFICATION", exception.getErrorCode().getCode());
        }
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .alias("alias")
                .emailAddress("user@gmail.com")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .active(true)
                .build();
    }

    private GmailHistoryEvent createEvent(UUID mailAccountId) {
        return new GmailHistoryEvent(GmailHistoryEventType.MESSAGE_ADDED, mailAccountId, "message-1", "thread-1", "history-1");
    }

    private InitialMailSyncMessageResult createMessageResult(String gmailMessageId, String subject, String snippet) {
        return new InitialMailSyncMessageResult(
                gmailMessageId,
                "thread-1",
                "history-1",
                Direction.INBOUND,
                subject,
                "sender@example.com",
                "Sender",
                List.of("user@gmail.com"),
                List.of("User"),
                List.of(),
                List.of(),
                snippet,
                false,
                LocalDateTime.of(2026, 4, 20, 12, 0),
                "body",
                null,
                List.of()
        );
    }

    private MailAccount eqMailAccount(MailAccount mailAccount) {
        return org.mockito.ArgumentMatchers.eq(mailAccount);
    }

    private GmailHistoryEvent eqEvent(GmailHistoryEvent event) {
        return org.mockito.ArgumentMatchers.eq(event);
    }

    private com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand anySyncCommand() {
        return org.mockito.ArgumentMatchers.any();
    }
}
