package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.mail.watch.RenewGoogleWatchCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailAccountCommandService 테스트")
class MailAccountCommandServiceTest {

    @Mock
    private MailAccountRepositoryPort mailAccountRepositoryPort;

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    private MailAccountCommandService service;

    @BeforeEach
    void setUp() {
        service = new MailAccountCommandService(mailAccountRepositoryPort, mailAccountQueryService);
    }

    @Nested
    @DisplayName("updateSyncHistoryId")
    class UpdateSyncHistoryId {

        @Test
        @DisplayName("유효한 historyId면 메일 계정의 sync history를 갱신한다")
        void updateSyncHistoryId_유효한HistoryId면메일계정의SyncHistory를갱신한다() {
            // given
            MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, "access-token", "refresh-token");

            // when
            service.updateSyncHistoryId(mailAccount, "history-2");

            // then
            assertEquals("history-2", mailAccount.getSyncHistoryId());
        }

        @Test
        @DisplayName("mailAccount가 없으면 예외를 반환한다")
        void updateSyncHistoryId_mailAccount가없으면예외를반환한다() {
            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.updateSyncHistoryId(null, "history-2")
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-PUSH-NOTIFICATION", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("renewGoogleWatch")
    class RenewGoogleWatch {

        @Test
        @DisplayName("refresh token이 비어 있으면 기존 refresh token을 유지한 채 watch를 갱신한다")
        void renewGoogleWatch_refreshToken이비어있으면기존RefreshToken을유지한채Watch를갱신한다() {
            // given
            MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, "old-access-token", "old-refresh-token");
            GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult("new-access-token", " ", 3600L, null, "Bearer");
            GoogleMailWatchResult watchResult = new GoogleMailWatchResult("history-123", LocalDateTime.of(2026, 4, 20, 12, 0));
            RenewGoogleWatchCommand command = RenewGoogleWatchCommand.of(mailAccount.getId(), tokenResult, watchResult);

            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(mailAccountQueryService.getKstNow()).willReturn(LocalDateTime.of(2026, 4, 20, 9, 0));

            // when
            service.renewGoogleWatch(command);

            // then
            then(mailAccountRepositoryPort).should().renewGoogleWatchIfAccessTokenMatches(
                    eq(mailAccount.getId()),
                    eq("old-access-token"),
                    eq("new-access-token"),
                    eq(LocalDateTime.of(2026, 4, 20, 10, 0)),
                    eq("old-refresh-token"),
                    eq("history-123"),
                    eq(LocalDateTime.of(2026, 4, 20, 12, 0))
            );
        }

        @Test
        @DisplayName("command가 비어 있으면 예외를 반환한다")
        void renewGoogleWatch_command가비어있으면예외를반환한다() {
            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.renewGoogleWatch(null)
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-WATCH-RENEWAL-REQUEST", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("gmail 계정이 아니면 예외를 반환한다")
        void renewGoogleWatch_gmail계정이아니면예외를반환한다() {
            // given
            MailAccount mailAccount = createMailAccount(MailProvider.NAVER, "old-access-token", "old-refresh-token");
            RenewGoogleWatchCommand command = RenewGoogleWatchCommand.of(
                    mailAccount.getId(),
                    new GoogleOAuthTokenResult("new-access-token", "new-refresh-token", 3600L, null, "Bearer"),
                    new GoogleMailWatchResult("history-123", LocalDateTime.of(2026, 4, 20, 12, 0))
            );
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.renewGoogleWatch(command)
            );

            // then
            assertEquals("MS-MAIL-INVALID-MAIL-ACCOUNT-STATE", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("refreshGoogleAccessToken")
    class RefreshGoogleAccessToken {

        @Test
        @DisplayName("유효한 토큰 결과면 access token을 갱신하고 최신 계정을 반환한다")
        void refreshGoogleAccessToken_유효한토큰결과면AccessToken을갱신하고최신계정을반환한다() {
            // given
            MailAccount currentMailAccount = createMailAccount(MailProvider.GMAIL, "old-access-token", "old-refresh-token");
            MailAccount refreshedMailAccount = createMailAccount(MailProvider.GMAIL, "new-access-token", "old-refresh-token");
            GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult("new-access-token", null, 1800L, null, "Bearer");
            given(mailAccountQueryService.findActiveMailAccountById(currentMailAccount.getId()))
                    .willReturn(currentMailAccount, refreshedMailAccount);
            given(mailAccountQueryService.getKstNow()).willReturn(LocalDateTime.of(2026, 4, 20, 9, 30));

            // when
            MailAccount result = service.refreshGoogleAccessToken(currentMailAccount.getId(), tokenResult);

            // then
            assertSame(refreshedMailAccount, result);
            then(mailAccountRepositoryPort).should().updateGoogleTokenIfAccessTokenMatches(
                    eq(currentMailAccount.getId()),
                    eq("old-access-token"),
                    eq("new-access-token"),
                    eq(LocalDateTime.of(2026, 4, 20, 10, 0)),
                    eq("old-refresh-token")
            );
        }

        @Test
        @DisplayName("expiresIn이 0 이하이면 예외를 반환한다")
        void refreshGoogleAccessToken_expiresIn이0이하면예외를반환한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.refreshGoogleAccessToken(
                            mailAccountId,
                            new GoogleOAuthTokenResult("new-access-token", "refresh-token", 0L, null, "Bearer")
                    )
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-REFRESH-FAILED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("refresh token이 없는 gmail 계정이면 예외를 반환한다")
        void refreshGoogleAccessToken_refreshToken이없는Gmail계정이면예외를반환한다() {
            // given
            MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, "old-access-token", " ");
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.refreshGoogleAccessToken(
                            mailAccount.getId(),
                            new GoogleOAuthTokenResult("new-access-token", null, 1800L, null, "Bearer")
                    )
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-REFRESH-TOKEN-MISSING", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("general")
    class General {

        @Test
        @DisplayName("repository update 결과값과 무관하게 예외 없이 종료한다")
        void repositoryUpdate결과값과무관하게예외없이종료한다() {
            // given
            MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, "old-access-token", "old-refresh-token");
            GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult("new-access-token", "new-refresh-token", 3600L, null, "Bearer");
            GoogleMailWatchResult watchResult = new GoogleMailWatchResult("history-321", LocalDateTime.of(2026, 4, 20, 15, 0));
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(mailAccountQueryService.getKstNow()).willReturn(LocalDateTime.of(2026, 4, 20, 9, 0));

            // when
            assertDoesNotThrow(() -> service.renewGoogleWatch(
                    RenewGoogleWatchCommand.of(mailAccount.getId(), tokenResult, watchResult)
            ));

            // then
            then(mailAccountRepositoryPort).should().renewGoogleWatchIfAccessTokenMatches(
                    eq(mailAccount.getId()),
                    eq("old-access-token"),
                    eq("new-access-token"),
                    any(LocalDateTime.class),
                    eq("new-refresh-token"),
                    eq("history-321"),
                    eq(LocalDateTime.of(2026, 4, 20, 15, 0))
            );
        }
    }

    private MailAccount createMailAccount(MailProvider provider, String accessToken, String refreshToken) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(provider)
                .emailAddress("user@example.com")
                .alias("alias")
                .icon("icon")
                .color("#4285F4")
                .accessToken(accessToken)
                .accessTokenExpiresAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .refreshToken(refreshToken)
                .syncHistoryId("history-1")
                .watchExpiresAt(LocalDateTime.of(2026, 4, 21, 9, 0))
                .active(true)
                .build();
    }
}
