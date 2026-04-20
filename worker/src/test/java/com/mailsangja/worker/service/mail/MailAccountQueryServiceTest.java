package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailAccountQueryService 테스트")
class MailAccountQueryServiceTest {

    @Mock
    private MailAccountRepositoryPort mailAccountRepositoryPort;

    @Nested
    @DisplayName("findActiveGoogleMailAccountByEmailAddress")
    class FindActiveGoogleMailAccountByEmailAddress {

        @Test
        @DisplayName("유효한 gmail 계정이면 그대로 반환한다")
        void findActiveGoogleMailAccountByEmailAddress_유효한Gmail계정이면그대로반환한다() {
            // given
            MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
            MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, "access-token", true);
            given(mailAccountRepositoryPort.findByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider.GMAIL, "user@gmail.com"))
                    .willReturn(Optional.of(mailAccount));

            // when
            MailAccount result = service.findActiveGoogleMailAccountByEmailAddress("user@gmail.com");

            // then
            assertSame(mailAccount, result);
        }

        @Test
        @DisplayName("access token이 비어 있으면 예외를 반환한다")
        void findActiveGoogleMailAccountByEmailAddress_accessToken이비어있으면예외를반환한다() {
            // given
            MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
            MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, " ", true);
            given(mailAccountRepositoryPort.findByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider.GMAIL, "user@gmail.com"))
                    .willReturn(Optional.of(mailAccount));

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.findActiveGoogleMailAccountByEmailAddress("user@gmail.com")
            );

            // then
            assertEquals("MS-MAIL-INVALID-MAIL-ACCOUNT-STATE", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("findRenewalTargetGmailAccounts")
    class FindRenewalTargetGmailAccounts {

        @Test
        @DisplayName("유효한 threshold와 limit면 repository 결과를 반환한다")
        void findRenewalTargetGmailAccounts_유효한Threshold와Limit면Repository결과를반환한다() {
            // given
            MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);
            LocalDateTime threshold = LocalDateTime.of(2026, 4, 20, 12, 0);
            List<MailAccount> accounts = List.of(createMailAccount(MailProvider.GMAIL, "access-token", true));
            given(mailAccountRepositoryPort.findRenewalTargetGmailAccounts(MailProvider.GMAIL, threshold, 10))
                    .willReturn(accounts);

            // when
            List<MailAccount> result = service.findRenewalTargetGmailAccounts(threshold, 10);

            // then
            assertEquals(1, result.size());
            assertSame(accounts, result);
        }

        @Test
        @DisplayName("limit이 0 이하이면 예외를 반환한다")
        void findRenewalTargetGmailAccounts_limit이0이하면예외를반환한다() {
            // given
            MailAccountQueryService service = new MailAccountQueryService(mailAccountRepositoryPort);

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.findRenewalTargetGmailAccounts(LocalDateTime.of(2026, 4, 20, 12, 0), 0)
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-WATCH-RENEWAL-REQUEST", exception.getErrorCode().getCode());
        }
    }

    private MailAccount createMailAccount(MailProvider provider, String accessToken, boolean active) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(provider)
                .emailAddress("user@gmail.com")
                .accessToken(accessToken)
                .refreshToken("refresh-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 4, 20, 12, 0))
                .active(active)
                .build();
    }
}
