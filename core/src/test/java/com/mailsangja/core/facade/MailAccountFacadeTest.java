package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.core.service.google.GoogleMailWatchQueryService;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.core.service.mail.InitialMailSyncMessageCommandService;
import com.mailsangja.core.service.mail.MailAccountCommandService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("MailAccountFacade 테스트")
class MailAccountFacadeTest {

    @Mock
    private MailAccountCommandService mailAccountCommandService;

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private GoogleOAuthQueryService googleOAuthQueryService;

    @Mock
    private GoogleMailWatchQueryService googleMailWatchQueryService;

    @Mock
    private InitialMailSyncMessageCommandService initialMailSyncMessageCommandService;

    @InjectMocks
    private MailAccountFacade mailAccountFacade;

    @Nested
    @DisplayName("authorizeGoogle")
    class AuthorizeGoogle {

        @Test
        @DisplayName("state를 사용해 인가 URL 응답을 생성한다")
        void authorizeGoogle_state를사용해인가Url응답을생성한다() {
            // given
            given(googleOAuthQueryService.buildAuthorizationUrl("state-1"))
                    .willReturn("https://accounts.google.com/o/oauth2/auth?state=state-1");

            // when
            MailAccountAuthorizeResponse response = mailAccountFacade.authorizeGoogle("state-1");

            // then
            assertEquals("https://accounts.google.com/o/oauth2/auth?state=state-1", response.authorizationUrl());
        }
    }

    @Nested
    @DisplayName("handleGoogleCallback")
    class HandleGoogleCallback {

        @Test
        @DisplayName("유효한 Gmail 계정이면 watch 등록 후 초기 동기화 메시지를 발행한다")
        void handleGoogleCallback_유효한Gmail계정이면Watch등록후초기동기화메시지를발행한다() {
            // given
            User user = createUser();
            GoogleMailAccountResult accountResult = new GoogleMailAccountResult(
                    "user@gmail.com",
                    "access-token",
                    LocalDateTime.of(2026, 4, 25, 12, 0),
                    "refresh-token"
            );
            GoogleMailWatchResult watchResult = new GoogleMailWatchResult(
                    "history-1",
                    LocalDateTime.of(2026, 4, 26, 12, 0)
            );
            MailAccount savedMailAccount = createMailAccount(user, MailProvider.GMAIL, "업무 메일", "mail", "#12AB34");

            given(googleOAuthQueryService.getGoogleMailAccountResult("code-1")).willReturn(accountResult);
            given(googleMailWatchQueryService.watch("access-token")).willReturn(watchResult);
            given(mailAccountCommandService.createGoogleMailAccount(
                    user,
                    accountResult,
                    "업무 메일",
                    "mail",
                    "#12AB34",
                    watchResult
            )).willReturn(savedMailAccount);

            // when
            MailAccountResponse response = mailAccountFacade.handleGoogleCallback(
                    user,
                    "code-1",
                    "업무 메일",
                    "mail",
                    "#12AB34"
            );

            // then
            assertEquals(savedMailAccount.getId(), response.id());
            assertEquals("업무 메일", response.alias());
            then(mailAccountCommandService).should().validateGoogleMailAccountCreation(user, accountResult);
            then(initialMailSyncMessageCommandService).should().publish(any());
        }

        @Test
        @DisplayName("인가 코드가 비어 있으면 예외를 반환한다")
        void handleGoogleCallback_인가코드가비어있으면예외를반환한다() {
            // given
            User user = createUser();

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountFacade.handleGoogleCallback(user, " ", "alias", "icon", "#12AB34")
            );

            // then
            assertEquals("MS-MAIL-INVALID-AUTHORIZATION-CODE", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("색상이 HEX 형식이 아니면 예외를 반환한다")
        void handleGoogleCallback_색상이Hex형식이아니면예외를반환한다() {
            // given
            User user = createUser();
            given(googleOAuthQueryService.getGoogleMailAccountResult("code-1")).willReturn(
                    new GoogleMailAccountResult("user@gmail.com", "access-token", LocalDateTime.now(), "refresh-token")
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountFacade.handleGoogleCallback(user, "code-1", "alias", "icon", "red")
            );

            // then
            assertEquals("MS-MAIL-INVALID-MAIL-ACCOUNT-COLOR", exception.getErrorCode().getCode());
        }
    }

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("tester")
                .username("tester@example.com")
                .password("encoded")
                .role(Role.USER)
                .plan(Plan.FREE)
                .build();
    }

    private MailAccount createMailAccount(User user, MailProvider provider, String alias, String icon, String color) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(provider)
                .emailAddress("user@gmail.com")
                .alias(alias)
                .icon(icon)
                .color(color)
                .active(true)
                .syncHistoryId("history-1")
                .watchExpiresAt(LocalDateTime.of(2026, 4, 26, 12, 0))
                .build();
    }
}
