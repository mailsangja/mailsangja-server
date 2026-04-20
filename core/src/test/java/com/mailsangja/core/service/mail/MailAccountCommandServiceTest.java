package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailAccountCommandService 테스트")
class MailAccountCommandServiceTest {

    @Mock
    private MailAccountRepositoryPort mailAccountRepositoryPort;

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @InjectMocks
    private MailAccountCommandService mailAccountCommandService;

    @Nested
    @DisplayName("구글 메일 계정 생성 검증")
    class ValidateGoogleMailAccountCreation {

        @Test
        @DisplayName("유효한 결과이면 중복 검사를 통과한다")
        void validateGoogleMailAccountCreation_유효한결과이면중복검사를통과한다() {
            // given
            User user = createUser();
            GoogleMailAccountResult result = createGoogleMailAccountResult();
            given(mailAccountQueryService.findByUserIdAndProviderAndEmailAddress(
                    user.getId(),
                    MailProvider.GMAIL,
                    result.emailAddress()
            )).willReturn(Optional.empty());
            given(mailAccountQueryService.findByProviderAndEmailAddress(
                    MailProvider.GMAIL,
                    result.emailAddress()
            )).willReturn(Optional.empty());

            // when
            mailAccountCommandService.validateGoogleMailAccountCreation(user, result);

            // then
            then(mailAccountQueryService).should()
                    .findByUserIdAndProviderAndEmailAddress(user.getId(), MailProvider.GMAIL, result.emailAddress());
            then(mailAccountQueryService).should()
                    .findByProviderAndEmailAddress(MailProvider.GMAIL, result.emailAddress());
        }

        @Test
        @DisplayName("같은 사용자가 이미 연결한 계정이면 예외를 반환한다")
        void validateGoogleMailAccountCreation_같은사용자가이미연결한계정이면예외를반환한다() {
            // given
            User user = createUser();
            GoogleMailAccountResult result = createGoogleMailAccountResult();
            given(mailAccountQueryService.findByUserIdAndProviderAndEmailAddress(
                    user.getId(),
                    MailProvider.GMAIL,
                    result.emailAddress()
            )).willReturn(Optional.of(createMailAccount(user, MailProvider.GMAIL)));

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountCommandService.validateGoogleMailAccountCreation(user, result)
            );

            // then
            assertEquals("MS-MAIL-ACCOUNT-ALREADY-CONNECTED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("다른 사용자가 이미 연결한 계정이면 예외를 반환한다")
        void validateGoogleMailAccountCreation_다른사용자가이미연결한계정이면예외를반환한다() {
            // given
            User user = createUser();
            User anotherUser = createUser();
            GoogleMailAccountResult result = createGoogleMailAccountResult();
            given(mailAccountQueryService.findByUserIdAndProviderAndEmailAddress(
                    user.getId(),
                    MailProvider.GMAIL,
                    result.emailAddress()
            )).willReturn(Optional.empty());
            given(mailAccountQueryService.findByProviderAndEmailAddress(
                    MailProvider.GMAIL,
                    result.emailAddress()
            )).willReturn(Optional.of(createMailAccount(anotherUser, MailProvider.GMAIL)));

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountCommandService.validateGoogleMailAccountCreation(user, result)
            );

            // then
            assertEquals("MS-MAIL-ACCOUNT-ALREADY-CONNECTED-BY-ANOTHER-USER", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("리프레시 토큰이 없으면 예외를 반환한다")
        void validateGoogleMailAccountCreation_리프레시토큰이없으면예외를반환한다() {
            // given
            User user = createUser();
            GoogleMailAccountResult result = new GoogleMailAccountResult(
                    "user@gmail.com",
                    "access-token",
                    LocalDateTime.of(2026, 4, 20, 12, 0),
                    " "
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountCommandService.validateGoogleMailAccountCreation(user, result)
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-REFRESH-TOKEN-MISSING", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("구글 메일 계정 생성")
    class CreateGoogleMailAccount {

        @Test
        @DisplayName("유효한 입력이면 메일 계정을 저장한다")
        void createGoogleMailAccount_유효한입력이면메일계정을저장한다() {
            // given
            User user = createUser();
            GoogleMailAccountResult result = createGoogleMailAccountResult();
            GoogleMailWatchResult watchResult = new GoogleMailWatchResult(
                    "history-1",
                    LocalDateTime.of(2026, 4, 21, 12, 0)
            );
            MailAccount savedMailAccount = createMailAccount(user, MailProvider.GMAIL);
            given(mailAccountRepositoryPort.save(any(MailAccount.class))).willReturn(savedMailAccount);

            // when
            MailAccount mailAccount = mailAccountCommandService.createGoogleMailAccount(
                    user,
                    result,
                    "업무 메일",
                    "mail",
                    "#123ABC",
                    watchResult
            );

            // then
            assertSame(savedMailAccount, mailAccount);
            ArgumentCaptor<MailAccount> captor = ArgumentCaptor.forClass(MailAccount.class);
            then(mailAccountRepositoryPort).should().save(captor.capture());
            assertEquals("user@gmail.com", captor.getValue().getEmailAddress());
            assertEquals("업무 메일", captor.getValue().getAlias());
            assertEquals("mail", captor.getValue().getIcon());
            assertEquals("#123ABC", captor.getValue().getColor());
            assertEquals("history-1", captor.getValue().getSyncHistoryId());
        }

        @Test
        @DisplayName("별칭이 비어 있으면 예외를 반환한다")
        void createGoogleMailAccount_별칭이비어있으면예외를반환한다() {
            // given
            User user = createUser();

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountCommandService.createGoogleMailAccount(
                            user,
                            createGoogleMailAccountResult(),
                            " ",
                            "mail",
                            "#123ABC",
                            new GoogleMailWatchResult("history-1", LocalDateTime.of(2026, 4, 21, 12, 0))
                    )
            );

            // then
            assertEquals("MS-MAIL-INVALID-OAUTH-RESULT", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("저장된 계정의 식별자가 없으면 예외를 반환한다")
        void createGoogleMailAccount_저장된계정의식별자가없으면예외를반환한다() {
            // given
            User user = createUser();
            MailAccount savedMailAccountWithoutId = MailAccount.builder()
                    .user(user)
                    .provider(MailProvider.GMAIL)
                    .emailAddress("user@gmail.com")
                    .alias("업무 메일")
                    .icon("mail")
                    .color("#123ABC")
                    .accessToken("access-token")
                    .accessTokenExpiresAt(LocalDateTime.of(2026, 4, 20, 12, 0))
                    .refreshToken("refresh-token")
                    .active(true)
                    .syncHistoryId("history-1")
                    .watchExpiresAt(LocalDateTime.of(2026, 4, 21, 12, 0))
                    .build();
            given(mailAccountRepositoryPort.save(any(MailAccount.class))).willReturn(savedMailAccountWithoutId);

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountCommandService.createGoogleMailAccount(
                            user,
                            createGoogleMailAccountResult(),
                            "업무 메일",
                            "mail",
                            "#123ABC",
                            new GoogleMailWatchResult("history-1", LocalDateTime.of(2026, 4, 21, 12, 0))
                    )
            );

            // then
            assertEquals("MS-MAIL-INVALID-OAUTH-RESULT", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("구글 액세스 토큰 갱신")
    class RefreshGoogleAccessToken {

        @Test
        @DisplayName("새 리프레시 토큰이 비어 있으면 기존 값을 유지한다")
        void refreshGoogleAccessToken_새리프레시토큰이비어있으면기존값을유지한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            MailAccount existingMailAccount = createMailAccount(createUser(), MailProvider.GMAIL);
            GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult(
                    "new-access-token",
                    " ",
                    3600L,
                    "scope",
                    "Bearer"
            );
            MailAccount refreshedMailAccount = createMailAccount(createUser(), MailProvider.GMAIL);
            given(mailAccountQueryService.findActiveById(mailAccountId))
                    .willReturn(existingMailAccount, refreshedMailAccount);
            given(mailAccountQueryService.getKstNow()).willReturn(LocalDateTime.of(2026, 4, 20, 12, 0));

            // when
            MailAccount result = mailAccountCommandService.refreshGoogleAccessToken(mailAccountId, tokenResult);

            // then
            assertSame(refreshedMailAccount, result);
            then(mailAccountRepositoryPort).should().updateGoogleTokenIfAccessTokenMatches(
                    mailAccountId,
                    existingMailAccount.getAccessToken(),
                    "new-access-token",
                    LocalDateTime.of(2026, 4, 20, 13, 0),
                    existingMailAccount.getRefreshToken()
            );
        }

        @Test
        @DisplayName("입력이 유효하지 않으면 갱신 실패 예외를 반환한다")
        void refreshGoogleAccessToken_입력이유효하지않으면갱신실패예외를반환한다() {
            // given
            GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult("", "refresh-token", 0L, "scope", "Bearer");

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountCommandService.refreshGoogleAccessToken(UUID.randomUUID(), tokenResult)
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-REFRESH-FAILED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("Gmail 계정이 아니면 예외를 반환한다")
        void refreshGoogleAccessToken_gmail계정이아니면예외를반환한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            given(mailAccountQueryService.findActiveById(mailAccountId))
                    .willReturn(createMailAccount(createUser(), MailProvider.NAVER));

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountCommandService.refreshGoogleAccessToken(
                            mailAccountId,
                            new GoogleOAuthTokenResult("new-access-token", "refresh-token", 3600L, "scope", "Bearer")
                    )
            );

            // then
            assertEquals("MS-MAIL-UNSUPPORTED-PROVIDER", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("기존 리프레시 토큰이 없으면 예외를 반환한다")
        void refreshGoogleAccessToken_기존리프레시토큰이없으면예외를반환한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            MailAccount mailAccount = MailAccount.builder()
                    .id(mailAccountId)
                    .user(createUser())
                    .provider(MailProvider.GMAIL)
                    .emailAddress("user@gmail.com")
                    .alias("업무 메일")
                    .icon("mail")
                    .color("#123ABC")
                    .accessToken("access-token")
                    .accessTokenExpiresAt(LocalDateTime.of(2026, 4, 20, 12, 0))
                    .refreshToken(" ")
                    .active(true)
                    .build();
            given(mailAccountQueryService.findActiveById(mailAccountId)).willReturn(mailAccount);

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> mailAccountCommandService.refreshGoogleAccessToken(
                            mailAccountId,
                            new GoogleOAuthTokenResult("new-access-token", "refresh-token", 3600L, "scope", "Bearer")
                    )
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-REFRESH-TOKEN-MISSING", exception.getErrorCode().getCode());
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

    private GoogleMailAccountResult createGoogleMailAccountResult() {
        return new GoogleMailAccountResult(
                "user@gmail.com",
                "access-token",
                LocalDateTime.of(2026, 4, 20, 12, 0),
                "refresh-token"
        );
    }

    private MailAccount createMailAccount(User user, MailProvider provider) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(provider)
                .emailAddress("user@gmail.com")
                .alias("업무 메일")
                .icon("mail")
                .color("#123ABC")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 4, 20, 12, 0))
                .refreshToken("saved-refresh-token")
                .active(true)
                .syncHistoryId("history-1")
                .watchExpiresAt(LocalDateTime.of(2026, 4, 21, 12, 0))
                .build();
    }
}
