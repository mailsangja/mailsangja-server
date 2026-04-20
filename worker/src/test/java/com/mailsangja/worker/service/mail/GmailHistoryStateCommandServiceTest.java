package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailHistoryStateCommandService 테스트")
class GmailHistoryStateCommandServiceTest {

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;

    @Mock
    private GmailHistoryStateQueryService gmailHistoryStateQueryService;

    @Mock
    private GoogleMailMessageQueryService googleMailMessageQueryService;

    @Mock
    private GmailHistoryStateApplyCommandService gmailHistoryStateApplyCommandService;

    private GmailHistoryStateCommandService service;

    @BeforeEach
    void setUp() {
        service = new GmailHistoryStateCommandService(
                mailAccountQueryService,
                googleAccessTokenEnsureService,
                gmailHistoryStateQueryService,
                googleMailMessageQueryService,
                gmailHistoryStateApplyCommandService
        );
    }

    @Nested
    @DisplayName("markMessageAsRead")
    class MarkMessageAsRead {

        @Test
        @DisplayName("이미 메시지가 있으면 추가 조회 없이 읽음 상태 적용을 위임한다")
        void markMessageAsRead_이미메시지가있으면추가조회없이읽음상태적용을위임한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(mailAccount.getId());
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).willReturn(mailAccount);
            given(gmailHistoryStateQueryService.existsMessage(mailAccount.getId(), "thread-1", "message-1")).willReturn(true);

            // when
            service.markMessageAsRead(event);

            // then
            then(googleMailMessageQueryService).shouldHaveNoInteractions();
            then(gmailHistoryStateApplyCommandService).should().applyMessageReadState(mailAccount, event, true, null);
        }

        @Test
        @DisplayName("메시지가 없으면 thread snapshot을 조회해 읽음 상태 적용을 위임한다")
        void markMessageAsRead_메시지가없으면ThreadSnapshot을조회해읽음상태적용을위임한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(mailAccount.getId());
            InitialMailSyncThreadResult threadResult = new InitialMailSyncThreadResult(
                    "thread-1",
                    "history-1",
                    List.of(new InitialMailSyncMessageResult(
                            "message-1",
                            "thread-1",
                            "history-1",
                            Direction.INBOUND,
                            "subject",
                            "sender@example.com",
                            "Sender",
                            List.of("user@gmail.com"),
                            List.of("User"),
                            List.of(),
                            List.of(),
                            "snippet",
                            false,
                            LocalDateTime.of(2026, 4, 20, 12, 0),
                            "body",
                            null,
                            List.of()
                    ))
            );
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).willReturn(mailAccount);
            given(gmailHistoryStateQueryService.existsMessage(mailAccount.getId(), "thread-1", "message-1")).willReturn(false);
            given(googleMailMessageQueryService.getThreads("access-token", List.of("thread-1"))).willReturn(List.of(threadResult));

            // when
            service.markMessageAsRead(event);

            // then
            then(gmailHistoryStateApplyCommandService).should().applyMessageReadState(eq(mailAccount), eq(event), eq(true), any());
        }
    }

    @Nested
    @DisplayName("markMessageAsUnread")
    class MarkMessageAsUnread {

        @Test
        @DisplayName("thread 조회 결과가 비어 있으면 예외를 반환한다")
        void markMessageAsUnread_thread조회결과가비어있으면예외를반환한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(mailAccount.getId());
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).willReturn(mailAccount);
            given(gmailHistoryStateQueryService.existsMessage(mailAccount.getId(), "thread-1", "message-1")).willReturn(false);
            given(googleMailMessageQueryService.getThreads("access-token", List.of("thread-1"))).willReturn(List.of());

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.markMessageAsUnread(event)
            );

            // then
            assertEquals("MS-MAIL-GMAIL-MESSAGES-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("event가 비어 있으면 예외를 반환한다")
        void markMessageAsUnread_event가비어있으면예외를반환한다() {
            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.markMessageAsUnread(null)
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-PUSH-NOTIFICATION", exception.getErrorCode().getCode());
        }
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@gmail.com")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .active(true)
                .build();
    }

    private GmailHistoryEvent createEvent(UUID mailAccountId) {
        return new GmailHistoryEvent(GmailHistoryEventType.MESSAGE_READ, mailAccountId, "message-1", "thread-1", "history-1");
    }
}
