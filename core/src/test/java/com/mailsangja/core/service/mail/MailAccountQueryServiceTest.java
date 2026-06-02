package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailAccountQueryServiceTest {

    @Test
    void findById_삭제되지않은메일계정을반환한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId))
                .thenReturn(Optional.of(mailAccount));

        // when
        MailAccount found = service.findById(mailAccountId);

        // then
        assertEquals(mailAccount, found);
    }

    @Test
    void findById_메일계정이없으면예외를던진다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(mailAccountId))
                .thenReturn(Optional.empty());

        // when
        MailAccountException exception = assertThrows(MailAccountException.class, () -> service.findById(mailAccountId));

        // then
        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findActiveById_활성메일계정을반환한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(mailAccountId, true))
                .thenReturn(Optional.of(mailAccount));

        // when
        MailAccount found = service.findActiveById(mailAccountId);

        // then
        assertEquals(mailAccount, found);
    }

    @Test
    void findActiveById_활성메일계정이없으면예외를던진다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(mailAccountId, true))
                .thenReturn(Optional.empty());

        // when
        MailAccountException exception = assertThrows(MailAccountException.class, () -> service.findActiveById(mailAccountId));

        // then
        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findActiveByUserIdAndEmailAddress_활성메일계정을반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(UUID.randomUUID());
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(
                userId,
                "user@example.com",
                true
        )).thenReturn(Optional.of(mailAccount));

        // when
        MailAccount found = service.findActiveByUserIdAndEmailAddress(userId, "user@example.com");

        // then
        assertEquals(mailAccount, found);
    }

    @Test
    void findActiveByUserIdAndEmailAddress_활성메일계정이없으면예외를던진다() {
        // given
        UUID userId = UUID.randomUUID();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(
                userId,
                "user@example.com",
                true
        )).thenReturn(Optional.empty());

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.findActiveByUserIdAndEmailAddress(userId, "user@example.com")
        );

        // then
        assertEquals(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findByUserIdAndProviderAndEmailAddress_포트결과를반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(UUID.randomUUID());
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(
                userId,
                MailProvider.GMAIL,
                "user@example.com"
        )).thenReturn(Optional.of(mailAccount));

        // when
        Optional<MailAccount> found = service.findByUserIdAndProviderAndEmailAddress(
                userId,
                MailProvider.GMAIL,
                "user@example.com"
        );

        // then
        assertTrue(found.isPresent());
        assertEquals(mailAccount, found.get());
    }

    @Test
    void findByProviderAndEmailAddress_포트결과를반환한다() {
        // given
        MailAccount mailAccount = createMailAccount(UUID.randomUUID());
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findByProviderAndEmailAddressAndDeletedAtIsNull(
                MailProvider.GMAIL,
                "user@example.com"
        )).thenReturn(Optional.of(mailAccount));

        // when
        Optional<MailAccount> found = service.findByProviderAndEmailAddress(MailProvider.GMAIL, "user@example.com");

        // then
        assertTrue(found.isPresent());
        assertEquals(mailAccount, found.get());
    }

    @Test
    void findAllByUserId_사용자의메일계정목록을반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        List<MailAccount> mailAccounts = List.of(createMailAccount(UUID.randomUUID()));
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findAllByUserIdAndDeletedAtIsNull(userId))
                .thenReturn(mailAccounts);

        // when
        List<MailAccount> found = service.findAllByUserId(userId);

        // then
        assertEquals(mailAccounts, found);
    }

    @Test
    void findAllActiveByUserId_사용자의활성메일계정목록을반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        List<MailAccount> mailAccounts = List.of(createMailAccount(UUID.randomUUID()));
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.findAllByUserIdAndActiveAndDeletedAtIsNull(userId, true))
                .thenReturn(mailAccounts);

        // when
        List<MailAccount> found = service.findAllActiveByUserId(userId);

        // then
        assertEquals(mailAccounts, found);
        verify(mailAccountRepositoryPort).findAllByUserIdAndActiveAndDeletedAtIsNull(userId, true);
    }

    @Test
    void existsOtherActiveGmailAccountByEmailAddress_다른활성Gmail계정이있으면true를반환한다() {
        // given
        UUID excludeUserId = UUID.randomUUID();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.existsByProviderAndEmailAddressAndUserIdNotAndDeletedAtIsNull(
                MailProvider.GMAIL, "user@example.com", excludeUserId
        )).thenReturn(true);

        // when
        boolean result = service.existsOtherActiveGmailAccountByEmailAddress(excludeUserId, "user@example.com");

        // then
        assertTrue(result);
    }

    @Test
    void existsOtherActiveGmailAccountByEmailAddress_다른활성Gmail계정이없으면false를반환한다() {
        // given
        UUID excludeUserId = UUID.randomUUID();
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
        when(mailAccountRepositoryPort.existsByProviderAndEmailAddressAndUserIdNotAndDeletedAtIsNull(
                MailProvider.GMAIL, "user@example.com", excludeUserId
        )).thenReturn(false);

        // when
        boolean result = service.existsOtherActiveGmailAccountByEmailAddress(excludeUserId, "user@example.com");

        // then
        assertFalse(result);
    }

    @Test
    void getKstNow_KST현재시각을반환한다() {
        // given
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);

        // when
        boolean present = service.getKstNow() != null;

        // then
        assertTrue(present);
        assertFalse(service.getKstNow().toString().isBlank());
    }

    private MailAccount createMailAccount(UUID id) {
        return MailAccount.builder()
                .id(id)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("alias")
                .icon("good")
                .color("#123456")
                .accessToken("access-token")
                .active(true)
                .build();
    }
}
