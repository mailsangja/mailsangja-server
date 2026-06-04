package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailAccountCommandServiceTest {

    // 2026-05-19T00:00:00Z = 2026-05-19T09:00:00 KST
    private static final Clock KST_FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void validateGoogleMailAccountCreation_중복계정이없으면통과한다() {
        // given
        User user = createUser("user@example.com");
        GoogleMailAccountResult result = createGoogleMailAccountResult();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(
                user.getId(),
                MailProvider.GMAIL,
                result.emailAddress()
        )).thenReturn(Optional.empty());

        // when
        service.validateGoogleMailAccountCreation(user, result);

        // then
        verify(mailAccountRepositoryPort).findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(
                user.getId(),
                MailProvider.GMAIL,
                result.emailAddress()
        );
    }

    @Test
    void validateGoogleMailAccountCreation_OAuth결과가null이면예외를던진다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.validateGoogleMailAccountCreation(createUser("user@example.com"), null)
        );

        // then
        assertEquals(MailAccountErrorCode.INVALID_OAUTH_RESULT, exception.getErrorCode());
    }

    @Test
    void validateGoogleMailAccountCreation_리프레시토큰이없으면예외를던진다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        GoogleMailAccountResult result = new GoogleMailAccountResult(
                "gmail@example.com",
                "access-token",
                LocalDateTime.of(2026, 5, 19, 10, 0),
                " "
        );

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.validateGoogleMailAccountCreation(createUser("user@example.com"), result)
        );

        // then
        assertEquals(MailAccountErrorCode.GOOGLE_REFRESH_TOKEN_MISSING, exception.getErrorCode());
    }

    @Test
    void validateGoogleMailAccountCreation_같은사용자중복계정이면예외를던진다() {
        // given
        User user = createUser("user@example.com");
        GoogleMailAccountResult result = createGoogleMailAccountResult();
        MailAccount existingMailAccount = createMailAccount(user, MailProvider.GMAIL, true, "access-token", "refresh-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(
                user.getId(), MailProvider.GMAIL, result.emailAddress()))
                .thenReturn(Optional.of(existingMailAccount));

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.validateGoogleMailAccountCreation(user, result)
        );

        // then
        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED, exception.getErrorCode());
    }

    @Test
    void createGoogleMailAccount_구글계정을생성하고저장한다() {
        // given
        User user = createUser("user@example.com");
        GoogleMailAccountResult result = createGoogleMailAccountResult();
        GoogleMailWatchResult watchResult = createGoogleMailWatchResult();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.save(any(MailAccount.class)))
                .thenAnswer(invocation -> {
                    MailAccount mailAccount = invocation.getArgument(0);
                    return MailAccount.builder()
                            .id(UUID.randomUUID())
                            .user(mailAccount.getUser())
                            .provider(mailAccount.getProvider())
                            .emailAddress(mailAccount.getEmailAddress())
                            .alias(mailAccount.getAlias())
                            .icon(mailAccount.getIcon())
                            .color(mailAccount.getColor())
                            .accessToken(mailAccount.getAccessToken())
                            .accessTokenExpiresAt(mailAccount.getAccessTokenExpiresAt())
                            .refreshToken(mailAccount.getRefreshToken())
                            .active(mailAccount.isActive())
                            .syncHistoryId(mailAccount.getSyncHistoryId())
                            .watchExpiresAt(mailAccount.getWatchExpiresAt())
                            .build();
                });

        // when
        MailAccount saved = service.createGoogleMailAccount(
                user,
                result,
                "업무용",
                "briefcase",
                "#123456",
                watchResult
        );

        // then
        ArgumentCaptor<MailAccount> mailAccountCaptor = ArgumentCaptor.forClass(MailAccount.class);
        verify(mailAccountRepositoryPort).save(mailAccountCaptor.capture());
        MailAccount persisted = mailAccountCaptor.getValue();
        assertEquals(user, persisted.getUser());
        assertEquals(MailProvider.GMAIL, persisted.getProvider());
        assertEquals(result.emailAddress(), persisted.getEmailAddress());
        assertEquals("업무용", persisted.getAlias());
        assertEquals("briefcase", persisted.getIcon());
        assertEquals("#123456", persisted.getColor());
        assertEquals(result.accessToken(), persisted.getAccessToken());
        assertEquals(result.accessTokenExpiresAt(), persisted.getAccessTokenExpiresAt());
        assertEquals(result.refreshToken(), persisted.getRefreshToken());
        assertEquals(watchResult.historyId(), persisted.getSyncHistoryId());
        assertEquals(watchResult.expirationAt(), persisted.getWatchExpiresAt());
        assertTrue(saved.isActive());
    }

    @Test
    void createGoogleMailAccount_watchHistoryId가없으면예외를던지고저장하지않는다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        GoogleMailWatchResult watchResult = new GoogleMailWatchResult(" ", LocalDateTime.of(2026, 5, 20, 9, 0));

        // when
        MailAccountException exception = assertThrows(MailAccountException.class, () -> service.createGoogleMailAccount(
                createUser("user@example.com"),
                createGoogleMailAccountResult(),
                "alias",
                "good",
                "#123456",
                watchResult
        ));

        // then
        assertEquals(MailAccountErrorCode.INVALID_OAUTH_RESULT, exception.getErrorCode());
        verify(mailAccountRepositoryPort, never()).save(any(MailAccount.class));
    }

    @Test
    void createGoogleMailAccount_저장결과Id가없으면예외를던진다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.save(any(MailAccount.class)))
                .thenReturn(MailAccount.builder().build());

        // when
        MailAccountException exception = assertThrows(MailAccountException.class, () -> service.createGoogleMailAccount(
                createUser("user@example.com"),
                createGoogleMailAccountResult(),
                "alias",
                "good",
                "#123456",
                createGoogleMailWatchResult()
        ));

        // then
        assertEquals(MailAccountErrorCode.INVALID_OAUTH_RESULT, exception.getErrorCode());
    }

    @Test
    void refreshGoogleAccessToken_새리프레시토큰이없으면기존리프레시토큰을유지하고갱신한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User user = createUser("user@example.com");
        MailAccount mailAccount = createMailAccount(user, MailProvider.GMAIL, true, "old-access-token", "old-refresh-token");
        MailAccount refreshedMailAccount = createMailAccount(user, MailProvider.GMAIL, true, "new-access-token", "old-refresh-token");
        GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult("new-access-token", " ", 3600L, null, "Bearer");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(mailAccountId, true))
                .thenReturn(Optional.of(mailAccount))
                .thenReturn(Optional.of(refreshedMailAccount));

        // when
        MailAccount result = service.refreshGoogleAccessToken(mailAccountId, tokenResult);

        // then
        assertEquals(refreshedMailAccount, result);
        verify(mailAccountRepositoryPort).updateGoogleTokenIfAccessTokenMatches(
                eq(mailAccountId),
                eq("old-access-token"),
                eq("new-access-token"),
                eq(LocalDateTime.of(2026, 5, 19, 10, 0)),
                eq("old-refresh-token")
        );
    }

    @Test
    void refreshGoogleAccessToken_새리프레시토큰이있으면함께갱신한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User user = createUser("user@example.com");
        MailAccount mailAccount = createMailAccount(user, MailProvider.GMAIL, true, "old-access-token", "old-refresh-token");
        GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult("new-access-token", "new-refresh-token", 60L, null, "Bearer");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(mailAccountId, true))
                .thenReturn(Optional.of(mailAccount))
                .thenReturn(Optional.of(mailAccount));

        // when
        service.refreshGoogleAccessToken(mailAccountId, tokenResult);

        // then
        verify(mailAccountRepositoryPort).updateGoogleTokenIfAccessTokenMatches(
                eq(mailAccountId),
                eq("old-access-token"),
                eq("new-access-token"),
                eq(LocalDateTime.of(2026, 5, 19, 9, 1)),
                eq("new-refresh-token")
        );
    }

    @Test
    void refreshGoogleAccessToken_입력이잘못되면예외를던지고갱신하지않는다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        GoogleOAuthTokenResult tokenResult = new GoogleOAuthTokenResult(" ", "refresh-token", 3600L, null, "Bearer");

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.refreshGoogleAccessToken(UUID.randomUUID(), tokenResult)
        );

        // then
        assertEquals(MailAccountErrorCode.GOOGLE_TOKEN_REFRESH_FAILED, exception.getErrorCode());
        verify(mailAccountRepositoryPort, never()).updateGoogleTokenIfAccessTokenMatches(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void refreshGoogleAccessToken_Gmail계정이아니면예외를던진다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User user = createUser("user@example.com");
        MailAccount mailAccount = createMailAccount(user, MailProvider.NAVER, true, "old-access-token", "refresh-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(mailAccountId, true))
                .thenReturn(Optional.of(mailAccount));

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.refreshGoogleAccessToken(mailAccountId, new GoogleOAuthTokenResult("new-token", null, 3600L, null, "Bearer"))
        );

        // then
        assertEquals(MailAccountErrorCode.UNSUPPORTED_MAIL_PROVIDER, exception.getErrorCode());
    }

    @Test
    void reauthorizeGoogleMailAccount_리프레시토큰이없는기존계정의토큰과watch정보를갱신한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User user = createUser("user@example.com");
        MailAccount mailAccount = createMailAccount(user, MailProvider.GMAIL, false, "old-access-token", null);
        GoogleMailAccountResult result = createGoogleMailAccountResult();
        GoogleMailWatchResult watchResult = createGoogleMailWatchResult();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId)).thenReturn(Optional.of(mailAccount));

        // when
        MailAccount reauthorized = service.reauthorizeGoogleMailAccount(user, mailAccountId, result, watchResult);

        // then
        assertEquals(mailAccount, reauthorized);
        assertEquals(result.accessToken(), mailAccount.getAccessToken());
        assertEquals(result.accessTokenExpiresAt(), mailAccount.getAccessTokenExpiresAt());
        assertEquals(result.refreshToken(), mailAccount.getRefreshToken());
        assertEquals(watchResult.historyId(), mailAccount.getSyncHistoryId());
        assertEquals(watchResult.expirationAt(), mailAccount.getWatchExpiresAt());
        assertFalse(mailAccount.isActive());
    }

    @Test
    void reauthorizeGoogleMailAccount_이미리프레시토큰이있으면예외를던진다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User user = createUser("user@example.com");
        MailAccount mailAccount = createMailAccount(user, MailProvider.GMAIL, true, "access-token", "refresh-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId)).thenReturn(Optional.of(mailAccount));

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.reauthorizeGoogleMailAccount(
                        user,
                        mailAccountId,
                        createGoogleMailAccountResult(),
                        createGoogleMailWatchResult()
                )
        );

        // then
        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_REAUTHORIZATION_NOT_REQUIRED, exception.getErrorCode());
    }

    @Test
    void reauthorizeGoogleMailAccount_OAuth계정이기존메일주소와다르면예외를던진다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User user = createUser("user@example.com");
        MailAccount mailAccount = createMailAccount(user, MailProvider.GMAIL, true, "access-token", null);
        GoogleMailAccountResult result = new GoogleMailAccountResult(
                "other@example.com",
                "access-token",
                LocalDateTime.of(2026, 5, 19, 10, 0),
                "refresh-token"
        );
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId)).thenReturn(Optional.of(mailAccount));

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.reauthorizeGoogleMailAccount(user, mailAccountId, result, createGoogleMailWatchResult())
        );

        // then
        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_REAUTHORIZATION_EMAIL_MISMATCH, exception.getErrorCode());
    }

    @Test
    void updateMailAccountAppearance_소유자이면blank이아닌값만갱신한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User user = createUser("user@example.com");
        MailAccount mailAccount = createMailAccount(user, MailProvider.GMAIL, true, "access-token", "refresh-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId)).thenReturn(Optional.of(mailAccount));

        // when
        service.updateMailAccountAppearance(user, mailAccountId, "새 별칭", " ", "#ABCDEF");

        // then
        assertEquals("새 별칭", mailAccount.getAlias());
        assertEquals("good", mailAccount.getIcon());
        assertEquals("#ABCDEF", mailAccount.getColor());
    }

    @Test
    void updateMailAccountAppearance_소유자가아니면예외를던진다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User owner = createUser("owner@example.com");
        User otherUser = createUser("other@example.com");
        MailAccount mailAccount = createMailAccount(owner, MailProvider.GMAIL, true, "access-token", "refresh-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId)).thenReturn(Optional.of(mailAccount));

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.updateMailAccountAppearance(otherUser, mailAccountId, "새 별칭", "good", "#ABCDEF")
        );

        // then
        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_ACCESS_DENIED, exception.getErrorCode());
        assertEquals("alias", mailAccount.getAlias());
    }

    @Test
    void deactivateMailAccount_소유자이면메일계정을비활성화한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User user = createUser("user@example.com");
        MailAccount mailAccount = createMailAccount(user, MailProvider.GMAIL, true, "access-token", "refresh-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId)).thenReturn(Optional.of(mailAccount));

        // when
        service.deactivateMailAccount(user, mailAccountId);

        // then
        assertFalse(mailAccount.isActive());
    }

    @Test
    void deactivateMailAccount_소유자가아니면예외를던진다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        User owner = createUser("owner@example.com");
        User otherUser = createUser("other@example.com");
        MailAccount mailAccount = createMailAccount(owner, MailProvider.GMAIL, true, "access-token", "refresh-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountCommandService service = new MailAccountCommandService(mailAccountRepositoryPort, KST_FIXED_CLOCK);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId)).thenReturn(Optional.of(mailAccount));

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.deactivateMailAccount(otherUser, mailAccountId)
        );

        // then
        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_ACCESS_DENIED, exception.getErrorCode());
        assertTrue(mailAccount.isActive());
    }

    private User createUser(String username) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("사용자")
                .username(username)
                .password("password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .build();
    }

    private GoogleMailAccountResult createGoogleMailAccountResult() {
        return new GoogleMailAccountResult(
                "gmail@example.com",
                "access-token",
                LocalDateTime.of(2026, 5, 19, 10, 0),
                "refresh-token"
        );
    }

    private GoogleMailWatchResult createGoogleMailWatchResult() {
        return new GoogleMailWatchResult(
                "history-id",
                LocalDateTime.of(2026, 5, 20, 9, 0)
        );
    }

    private MailAccount createMailAccount(
            User user,
            MailProvider provider,
            boolean active,
            String accessToken,
            String refreshToken
    ) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(provider)
                .emailAddress("gmail@example.com")
                .alias("alias")
                .icon("good")
                .color("#123456")
                .accessToken(accessToken)
                .accessTokenExpiresAt(LocalDateTime.of(2026, 5, 19, 10, 0))
                .refreshToken(refreshToken)
                .active(active)
                .syncHistoryId("history-id")
                .watchExpiresAt(LocalDateTime.of(2026, 5, 20, 9, 0))
                .build();
    }
}
