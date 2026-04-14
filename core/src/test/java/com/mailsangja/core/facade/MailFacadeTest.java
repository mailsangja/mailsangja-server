package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.dto.mail.MailComposeResponse;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.service.mail.MailCommandService;
import com.mailsangja.core.service.mail.MailQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailFacadeTest {

    @Test
    void createCompose_composeSessionId를가진응답을반환한다() {
        MailFacade mailFacade = createMailFacade(List.of());

        MailComposeResponse response = mailFacade.createCompose(createUser(UUID.randomUUID()));

        assertDoesNotThrow(() -> UUID.fromString(response.composeSessionId()));
    }

    @Test
    void sendMail_내활성메일계정이면검증을통과한다() {
        User user = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount));

        MailSendRequest request = new MailSendRequest(
                UUID.randomUUID().toString(),
                "sender@example.com",
                List.of("to@example.com"),
                List.of("cc@example.com"),
                List.of("bcc@example.com"),
                "",
                "본문",
                List.of(new MockMultipartFile("attachments", "file.txt", "text/plain", "hello".getBytes()))
        );

        assertDoesNotThrow(() -> mailFacade.sendMail(user, request));
    }

    @Test
    void sendMail_composeSessionId가유효하지않으면실패한다() {
        User user = createUser(UUID.randomUUID());
        MailFacade mailFacade = createMailFacade(List.of());

        MailSendRequest request = new MailSendRequest(
                "not-a-uuid",
                "sender@example.com",
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "",
                null
        );

        assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
    }

    @Test
    void sendMail_subject와content가둘다비어있으면실패한다() {
        User user = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount));

        MailSendRequest request = new MailSendRequest(
                UUID.randomUUID().toString(),
                "sender@example.com",
                List.of("to@example.com"),
                null,
                null,
                " ",
                " ",
                null
        );

        assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
    }

    @Test
    void sendMail_중복된수신자가있으면실패한다() {
        User user = createUser(UUID.randomUUID());
        MailFacade mailFacade = createMailFacade(List.of());

        MailSendRequest request = new MailSendRequest(
                UUID.randomUUID().toString(),
                "sender@example.com",
                List.of("dup@example.com"),
                List.of("dup@example.com"),
                null,
                "제목",
                "",
                null
        );

        assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
    }

    @Test
    void sendMail_내메일계정이아니면실패한다() {
        User owner = createUser(UUID.randomUUID());
        User anotherUser = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(owner, "sender@example.com", true);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount));

        MailSendRequest request = new MailSendRequest(
                UUID.randomUUID().toString(),
                "sender@example.com",
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "",
                null
        );

        assertThrows(MailSendException.class, () -> mailFacade.sendMail(anotherUser, request));
    }

    @Test
    void sendMail_첨부총합이20메가바이트를초과하면실패한다() {
        User user = createUser(UUID.randomUUID());
        MailFacade mailFacade = createMailFacade(List.of());

        byte[] oversized = new byte[11 * 1024 * 1024];
        MailSendRequest request = new MailSendRequest(
                UUID.randomUUID().toString(),
                "sender@example.com",
                List.of("to@example.com"),
                null,
                null,
                "제목",
                "",
                List.of(
                        new MockMultipartFile("attachments", "a.bin", "application/octet-stream", oversized),
                        new MockMultipartFile("attachments", "b.bin", "application/octet-stream", oversized)
                )
        );

        assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
    }

    private MailFacade createMailFacade(List<MailAccount> mailAccounts) {
        MailQueryService mailQueryService = new MailQueryService(new FakeMailAccountRepositoryPort(mailAccounts));
        MailCommandService mailCommandService = new MailCommandService(mailQueryService);
        return new MailFacade(mailCommandService);
    }

    private User createUser(UUID userId) {
        return User.builder()
                .id(userId)
                .name("tester")
                .username("tester@example.com")
                .password("password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .creditUsage(0)
                .build();
    }

    private MailAccount createMailAccount(User user, String emailAddress, boolean active) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress(emailAddress)
                .alias("alias")
                .icon("icon")
                .color("#4285F4")
                .accessToken("token")
                .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .refreshToken("refresh")
                .active(active)
                .build();
    }

    private static class FakeMailAccountRepositoryPort implements MailAccountRepositoryPort {

        private final List<MailAccount> mailAccounts;

        private FakeMailAccountRepositoryPort(List<MailAccount> mailAccounts) {
            this.mailAccounts = mailAccounts;
        }

        @Override
        public MailAccount save(MailAccount mailAccount) {
            return mailAccount;
        }

        @Override
        public Optional<MailAccount> findByIdAndDeletedAtIsNull(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByIdAndActiveAndDeletedAtIsNull(UUID id, boolean active) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByEmailAddressAndDeletedAtIsNull(String emailAddress) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByUserIdAndProviderAndDeletedAtIsNull(UUID userId, MailProvider provider) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(
                UUID userId,
                MailProvider provider,
                String emailAddress
        ) {
            return Optional.empty();
        }

        @Override
        public Optional<MailAccount> findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(
                UUID userId,
                String emailAddress,
                boolean active
        ) {
            return mailAccounts.stream()
                    .filter(mailAccount -> mailAccount.getUser() != null)
                    .filter(mailAccount -> userId.equals(mailAccount.getUser().getId()))
                    .filter(mailAccount -> emailAddress.equalsIgnoreCase(mailAccount.getEmailAddress()))
                    .filter(mailAccount -> mailAccount.isActive() == active)
                    .findFirst();
        }

        @Override
        public Optional<MailAccount> findByProviderAndEmailAddressAndDeletedAtIsNull(MailProvider provider, String emailAddress) {
            return Optional.empty();
        }

        @Override
        public List<MailAccount> findAllByUserIdAndDeletedAtIsNull(UUID userId) {
            return List.of();
        }

        @Override
        public List<MailAccount> findRenewalTargetGmailAccounts(MailProvider provider, LocalDateTime watchExpiresAtThreshold, int limit) {
            return List.of();
        }

        @Override
        public List<MailAccount> findAllByUserIdAndActiveAndDeletedAtIsNull(UUID userId, boolean active) {
            return List.of();
        }
    }
}
