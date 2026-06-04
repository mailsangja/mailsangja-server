package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailAccountQueryServiceTest {

    @Test
    void findSyncableGoogleMailAccountsByEmailAddress_동기화가능한Gmail계정을반환한다() {
        // given
        MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, true, "access-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findAllByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider.GMAIL, "user@example.com"))
                .thenReturn(List.of(mailAccount));

        // when
        List<MailAccount> found = service.findSyncableGoogleMailAccountsByEmailAddress("user@example.com");

        // then
        assertEquals(1, found.size());
        assertEquals(mailAccount, found.get(0));
    }

    @Test
    void findSyncableGoogleMailAccountsByEmailAddress_다중사용자Gmail계정을모두반환한다() {
        // given
        MailAccount account1 = createMailAccount(MailProvider.GMAIL, true, "token-1");
        MailAccount account2 = createMailAccount(MailProvider.GMAIL, true, "token-2");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findAllByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider.GMAIL, "user@example.com"))
                .thenReturn(List.of(account1, account2));

        // when
        List<MailAccount> found = service.findSyncableGoogleMailAccountsByEmailAddress("user@example.com");

        // then
        assertEquals(2, found.size());
    }

    @Test
    void findSyncableGoogleMailAccountsByEmailAddress_계정이없으면예외를던진다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findAllByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider.GMAIL, "user@example.com"))
                .thenReturn(List.of());

        // when
        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.findSyncableGoogleMailAccountsByEmailAddress("user@example.com")
        );

        // then
        assertEquals(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findSyncableGoogleMailAccountsByEmailAddress_동기화불가계정은필터링한다() {
        // given
        MailAccount unsyncableAccount = createMailAccount(MailProvider.GMAIL, true, " ");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findAllByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider.GMAIL, "user@example.com"))
                .thenReturn(List.of(unsyncableAccount));

        // when
        List<MailAccount> found = service.findSyncableGoogleMailAccountsByEmailAddress("user@example.com");

        // then
        assertTrue(found.isEmpty());
    }

    @Test
    void findSyncableGoogleMailAccountsByEmailAddress_리프레시토큰없는계정은필터링한다() {
        // given
        MailAccount refreshTokenMissingAccount = createMailAccount(MailProvider.GMAIL, true, "access-token");
        refreshTokenMissingAccount.clearRefreshToken();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findAllByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider.GMAIL, "user@example.com"))
                .thenReturn(List.of(refreshTokenMissingAccount));

        // when
        List<MailAccount> found = service.findSyncableGoogleMailAccountsByEmailAddress("user@example.com");

        // then
        assertTrue(found.isEmpty());
    }

    @Test
    void findSyncableMailAccountById_동기화가능한메일계정을반환한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, true, "access-token");
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId))
                .thenReturn(Optional.of(mailAccount));

        // when
        MailAccount found = service.findSyncableMailAccountById(mailAccountId);

        // then
        assertEquals(mailAccount, found);
    }

    @Test
    void findSyncableMailAccountById_계정이없으면예외를던진다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId))
                .thenReturn(Optional.empty());

        // when
        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.findSyncableMailAccountById(mailAccountId)
        );

        // then
        assertEquals(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findRenewalTargetGmailAccounts_갱신대상계정을조회한다() {
        // given
        LocalDateTime threshold = LocalDateTime.of(2026, 5, 19, 9, 0);
        List<MailAccount> mailAccounts = List.of(createMailAccount(MailProvider.GMAIL, true, "access-token"));
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findRenewalTargetGmailAccounts(MailProvider.GMAIL, threshold, 100))
                .thenReturn(mailAccounts);

        // when
        List<MailAccount> found = service.findRenewalTargetGmailAccounts(threshold, 100);

        // then
        assertEquals(mailAccounts, found);
        verify(mailAccountRepositoryPort).findRenewalTargetGmailAccounts(MailProvider.GMAIL, threshold, 100);
    }

    @Test
    void findRenewalTargetGmailAccounts_조회조건이잘못되면예외를던진다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);

        // when
        MailPushException exception = assertThrows(
                MailPushException.class,
                () -> service.findRenewalTargetGmailAccounts(null, 100)
        );

        // then
        assertEquals(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST, exception.getErrorCode());
    }

    @Test
    void getKstNow_KST현재시각을반환한다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);

        // when
        LocalDateTime now = service.getKstNow();

        // then
        assertTrue(now != null);
    }

    private MailAccount createMailAccount(MailProvider provider, boolean active, String accessToken) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(provider)
                .emailAddress("user@example.com")
                .alias("alias")
                .icon("good")
                .color("#123456")
                .accessToken(accessToken)
                .refreshToken("refresh-token")
                .active(active)
                .build();
    }
}
