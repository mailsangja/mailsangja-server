package com.mailsangja.worker.facade;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.mail.watch.WatchRenewalMessage;
import com.mailsangja.worker.service.google.GoogleMailWatchQueryService;
import com.mailsangja.worker.service.google.GoogleOAuthQueryService;
import com.mailsangja.worker.service.mail.MailAccountCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("worker MailAccountFacade 테스트")
class MailAccountFacadeTest {

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private MailAccountCommandService mailAccountCommandService;

    @Mock
    private GoogleOAuthQueryService googleOAuthQueryService;

    @Mock
    private GoogleMailWatchQueryService googleMailWatchQueryService;

    @InjectMocks
    private MailAccountFacade mailAccountFacade;

    @Nested
    @DisplayName("handleWatchRenewal")
    class HandleWatchRenewal {

        @Test
        @DisplayName("유효한 Gmail 메시지면 토큰 갱신 후 watch 갱신을 저장한다")
        void handleWatchRenewal_유효한Gmail메시지면토큰갱신후Watch갱신을저장한다() {
            // given
            MailAccount mailAccount = MailAccount.builder()
                    .id(UUID.randomUUID())
                    .provider(MailProvider.GMAIL)
                    .emailAddress("user@gmail.com")
                    .refreshToken("refresh-token")
                    .build();
            WatchRenewalMessage message = new WatchRenewalMessage(
                    mailAccount.getId(),
                    UUID.randomUUID(),
                    "GMAIL",
                    mailAccount.getEmailAddress()
            );
            GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult("new-token", "refresh-token", 3600L, null, "Bearer");
            GoogleMailWatchResult watchResult = new GoogleMailWatchResult("history-1", LocalDateTime.of(2026, 4, 26, 12, 0));

            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(googleOAuthQueryService.refreshAccessToken("refresh-token")).willReturn(tokenResult);
            given(googleMailWatchQueryService.watch("new-token")).willReturn(watchResult);

            // when
            mailAccountFacade.handleWatchRenewal(message);

            // then
            then(mailAccountCommandService).should().renewGoogleWatch(any());
        }

        @Test
        @DisplayName("provider가 Gmail이 아니면 예외를 반환한다")
        void handleWatchRenewal_provider가Gmail이아니면예외를반환한다() {
            // given
            WatchRenewalMessage message = new WatchRenewalMessage(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "NAVER",
                    "user@naver.com"
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> mailAccountFacade.handleWatchRenewal(message)
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-WATCH-RENEWAL-COMMAND", exception.getErrorCode().getCode());
        }
    }
}
