package com.mailsangja.worker.service.trash;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailHistoryDeleteCommandService 테스트")
class GmailHistoryDeleteCommandServiceTest {

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private GmailHistoryDeleteApplyCommandService gmailHistoryDeleteApplyCommandService;

    private GmailHistoryDeleteCommandService service;

    @BeforeEach
    void setUp() {
        service = new GmailHistoryDeleteCommandService(mailAccountQueryService, gmailHistoryDeleteApplyCommandService);
    }

    @Nested
    @DisplayName("trashMessage")
    class TrashMessage {

        @Test
        @DisplayName("유효한 event면 휴지통 적용을 위임한다")
        void trashMessage_유효한Event면휴지통적용을위임한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(mailAccount.getId());
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);

            // when
            service.trashMessage(event);

            // then
            then(gmailHistoryDeleteApplyCommandService).should().applyMessageTrashed(mailAccount, event);
        }

        @Test
        @DisplayName("event가 비어 있으면 예외를 반환한다")
        void trashMessage_event가비어있으면예외를반환한다() {
            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.trashMessage(null)
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-PUSH-NOTIFICATION", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("restoreMessage")
    class RestoreMessage {

        @Test
        @DisplayName("유효한 event면 복원 적용을 위임한다")
        void restoreMessage_유효한Event면복원적용을위임한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(mailAccount.getId());
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);

            // when
            service.restoreMessage(event);

            // then
            then(gmailHistoryDeleteApplyCommandService).should().applyMessageRestored(mailAccount, event);
        }
    }

    @Nested
    @DisplayName("permanentlyDeleteMessage")
    class PermanentlyDeleteMessage {

        @Test
        @DisplayName("유효한 event면 영구 삭제 적용을 위임한다")
        void permanentlyDeleteMessage_유효한Event면영구삭제적용을위임한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(mailAccount.getId());
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);

            // when
            service.permanentlyDeleteMessage(event);

            // then
            then(gmailHistoryDeleteApplyCommandService).should().applyMessagePermanentlyDeleted(mailAccount, event);
        }
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@gmail.com")
                .alias("alias")
                .icon("icon")
                .color("#4285F4")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 4, 20, 12, 0))
                .refreshToken("refresh-token")
                .active(true)
                .build();
    }

    private GmailHistoryEvent createEvent(UUID mailAccountId) {
        return new GmailHistoryEvent(GmailHistoryEventType.MESSAGE_TRASHED, mailAccountId, "message-1", "thread-1", "history-1");
    }
}
