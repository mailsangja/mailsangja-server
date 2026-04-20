package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.config.properties.GoogleOAuthProperties;
import com.mailsangja.core.dto.mail.GoogleMailAttachmentResult;
import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
import com.mailsangja.core.dto.mail.GoogleMailSendResult;
import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.core.dto.mail.MailAttachmentDownloadResult;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.service.google.GoogleMailAttachmentQueryService;
import com.mailsangja.core.service.google.GoogleMailMessageQueryService;
import com.mailsangja.core.service.google.GoogleMailSendCommandService;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.MailAccountCommandService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.mail.MailAttachmentQueryService;
import com.mailsangja.core.service.mail.MailCommandService;
import com.mailsangja.core.service.mail.MailQueryService;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("MailFacade 테스트")
class MailFacadeTest {

    @Nested
    @DisplayName("sendMail")
    class SendMail {

        @Test
        @DisplayName("내 활성 메일 계정이면 검증을 통과한다")
        void sendMail_내활성메일계정이면검증을통과한다() {
            // given
            User user = createUser(UUID.randomUUID());
            MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
            MailFacade mailFacade = createMailFacade(List.of(mailAccount));

            MailSendRequest request = new MailSendRequest(
                    "\"Sender\" <sender@example.com>",
                    List.of("\"To\" <to@example.com>"),
                    List.of("\"Cc\" <cc@example.com>"),
                    List.of("\"Bcc\" <bcc@example.com>"),
                    "",
                    "본문",
                    List.of(new MockMultipartFile("attachments", "file.txt", "text/plain", "hello".getBytes()))
            );

            // when then
            assertDoesNotThrow(() -> mailFacade.sendMail(user, request));
        }

        @Test
        @DisplayName("subject와 content가 둘 다 비어 있으면 실패한다")
        void sendMail_subject와Content가둘다비어있으면실패한다() {
            // given
            User user = createUser(UUID.randomUUID());
            MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
            MailFacade mailFacade = createMailFacade(List.of(mailAccount));

            MailSendRequest request = new MailSendRequest(
                    "\"Sender\" <sender@example.com>",
                    List.of("\"To\" <to@example.com>"),
                    null,
                    null,
                    " ",
                    " ",
                    null
            );

            // when then
            assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
        }

        @Test
        @DisplayName("subject에 개행 문자가 있으면 실패한다")
        void sendMail_subject에개행문자가있으면실패한다() {
            // given
            User user = createUser(UUID.randomUUID());
            MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
            MailFacade mailFacade = createMailFacade(List.of(mailAccount));

            MailSendRequest request = new MailSendRequest(
                    "\"Sender\" <sender@example.com>",
                    List.of("\"To\" <to@example.com>"),
                    null,
                    null,
                    "제목\n추가",
                    "본문",
                    null
            );

            // when then
            assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
        }

        @Test
        @DisplayName("중복된 수신자가 있으면 실패한다")
        void sendMail_중복된수신자가있으면실패한다() {
            // given
            User user = createUser(UUID.randomUUID());
            MailFacade mailFacade = createMailFacade(List.of());

            MailSendRequest request = new MailSendRequest(
                    "\"Sender\" <sender@example.com>",
                    List.of("\"수신자A\" <dup@example.com>"),
                    List.of("\"수신자B\" <dup@example.com>"),
                    null,
                    "제목",
                    "",
                    null
            );

            // when then
            assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
        }

        @Test
        @DisplayName("to가 비어 있고 cc만 있어도 실패한다")
        void sendMail_to가비어있고Cc만있어도실패한다() {
            // given
            User user = createUser(UUID.randomUUID());
            MailFacade mailFacade = createMailFacade(List.of());

            MailSendRequest request = new MailSendRequest(
                    "\"Sender\" <sender@example.com>",
                    List.of(),
                    List.of("\"Cc\" <cc@example.com>"),
                    null,
                    "제목",
                    "본문",
                    null
            );

            // when then
            assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
        }

        @Test
        @DisplayName("내 메일 계정이 아니면 실패한다")
        void sendMail_내메일계정이아니면실패한다() {
            // given
            User owner = createUser(UUID.randomUUID());
            User anotherUser = createUser(UUID.randomUUID());
            MailAccount mailAccount = createMailAccount(owner, "sender@example.com", true);
            MailFacade mailFacade = createMailFacade(List.of(mailAccount));

            MailSendRequest request = new MailSendRequest(
                    "\"Sender\" <sender@example.com>",
                    List.of("\"To\" <to@example.com>"),
                    null,
                    null,
                    "제목",
                    "",
                    null
            );

            // when then
            assertThrows(MailSendException.class, () -> mailFacade.sendMail(anotherUser, request));
        }

        @Test
        @DisplayName("첨부 총합이 20MB를 초과하면 실패한다")
        void sendMail_첨부총합이20메가바이트를초과하면실패한다() {
            // given
            User user = createUser(UUID.randomUUID());
            MailFacade mailFacade = createMailFacade(List.of());

            byte[] oversized = new byte[11 * 1024 * 1024];
            MailSendRequest request = new MailSendRequest(
                    "\"Sender\" <sender@example.com>",
                    List.of("\"To\" <to@example.com>"),
                    null,
                    null,
                    "제목",
                    "",
                    List.of(
                            new MockMultipartFile("attachments", "a.bin", "application/octet-stream", oversized),
                            new MockMultipartFile("attachments", "b.bin", "application/octet-stream", oversized)
                    )
            );

            // when then
            assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
        }
    }

    @Nested
    @DisplayName("getAttachment")
    class GetAttachment {

        @Test
        @DisplayName("내 활성 메일 계정의 지메일 첨부파일이면 다운로드한다")
        void getAttachment_내활성메일계정의지메일첨부파일이면다운로드한다() {
            // given
            User user = createUser(UUID.randomUUID());
            MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
            Attachment attachment = createAttachment(mailAccount, MailProvider.GMAIL);
            MailFacade mailFacade = createMailFacade(List.of(mailAccount), List.of(attachment));

            // when
            MailAttachmentDownloadResult result = mailFacade.getAttachment(user, attachment.getId());

            // then
            assertArrayEquals("file-content".getBytes(), result.bytes());
            assertEquals("text/plain", result.mimeType());
        }

        @Test
        @DisplayName("다른 사용자 계정의 첨부파일이면 실패한다")
        void getAttachment_다른사용자계정의첨부파일이면실패한다() {
            // given
            User owner = createUser(UUID.randomUUID());
            User anotherUser = createUser(UUID.randomUUID());
            MailAccount mailAccount = createMailAccount(owner, "sender@example.com", true);
            Attachment attachment = createAttachment(mailAccount, MailProvider.GMAIL);
            MailFacade mailFacade = createMailFacade(List.of(mailAccount), List.of(attachment));

            // when then
            assertThrows(InboxException.class, () -> mailFacade.getAttachment(anotherUser, attachment.getId()));
        }

        @Test
        @DisplayName("지메일이 아닌 첨부파일이면 실패한다")
        void getAttachment_지메일이아닌첨부파일이면실패한다() {
            // given
            User user = createUser(UUID.randomUUID());
            MailAccount mailAccount = createMailAccount(user, "sender@example.com", true, MailProvider.NAVER);
            Attachment attachment = createAttachment(mailAccount, MailProvider.NAVER);
            MailFacade mailFacade = createMailFacade(List.of(mailAccount), List.of(attachment));

            // when then
            assertThrows(InboxException.class, () -> mailFacade.getAttachment(user, attachment.getId()));
        }
    }

    private MailFacade createMailFacade(List<MailAccount> mailAccounts) {
        return createMailFacade(mailAccounts, List.of());
    }

    private MailFacade createMailFacade(List<MailAccount> mailAccounts, List<Attachment> attachments) {
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        AttachmentRepositoryPort attachmentRepositoryPort = mock(AttachmentRepositoryPort.class);
        ThreadRepositoryPort threadRepositoryPort = mock(ThreadRepositoryPort.class);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort = mock(GmailThreadLockRepositoryPort.class);

        given(mailAccountRepositoryPort.findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(any(), anyString(), eq(true)))
                .willAnswer(invocation -> mailAccounts.stream()
                        .filter(mailAccount -> mailAccount.getUser() != null)
                        .filter(mailAccount -> invocation.getArgument(0).equals(mailAccount.getUser().getId()))
                        .filter(mailAccount -> invocation.getArgument(1).equals(mailAccount.getEmailAddress()))
                        .filter(MailAccount::isActive)
                        .findFirst());
        given(mailAccountRepositoryPort.findAllByUserIdAndActiveAndDeletedAtIsNull(any(), eq(true)))
                .willAnswer(invocation -> mailAccounts.stream()
                        .filter(mailAccount -> mailAccount.getUser() != null)
                        .filter(mailAccount -> invocation.getArgument(0).equals(mailAccount.getUser().getId()))
                        .filter(MailAccount::isActive)
                        .toList());
        given(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(any(), eq(true)))
                .willAnswer(invocation -> mailAccounts.stream()
                        .filter(mailAccount -> invocation.getArgument(0).equals(mailAccount.getId()))
                        .filter(mailAccount -> mailAccount.isActive() == (boolean) invocation.getArgument(1))
                        .findFirst());
        given(attachmentRepositoryPort.findByIdAndDeletedAtIsNull(any()))
                .willAnswer(invocation -> attachments.stream()
                        .filter(attachment -> invocation.getArgument(0).equals(attachment.getId()))
                        .findFirst());
        given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(any(), anyString(), any()))
                .willReturn(Optional.empty());
        given(threadRepositoryPort.save(any(Thread.class))).willAnswer(invocation -> {
            Thread thread = invocation.getArgument(0);
            if (thread.getId() != null) {
                return thread;
            }
            return Thread.builder()
                    .id(UUID.randomUUID())
                    .mailAccount(thread.getMailAccount())
                    .gmailThreadId(thread.getGmailThreadId())
                    .direction(thread.getDirection())
                    .historyId(thread.getHistoryId())
                    .latestSubject(thread.getLatestSubject())
                    .latestSnippet(thread.getLatestSnippet())
                    .latestParticipantAddress(thread.getLatestParticipantAddress())
                    .latestParticipantName(thread.getLatestParticipantName())
                    .lastMessageAt(thread.getLastMessageAt())
                    .read(thread.isRead())
                    .messageCount(thread.getMessageCount())
                    .build();
        });
        given(messageRepositoryPort.findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(any(), anyString()))
                .willReturn(Optional.empty());
        given(messageRepositoryPort.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

        MailQueryService mailQueryService = new MailQueryService(mailAccountRepositoryPort);
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(mailAccountRepositoryPort);
        GoogleAccessTokenEnsureService googleAccessTokenEnsureService = new GoogleAccessTokenEnsureService(
                mailAccountQueryService,
                new MailAccountCommandService(mailAccountRepositoryPort, mailAccountQueryService),
                new FakeGoogleOAuthQueryService()
        );
        MailCommandService mailCommandService = new MailCommandService(
                mailQueryService,
                googleAccessTokenEnsureService,
                new FakeGoogleMailSendCommandService(),
                new FakeGoogleMailMessageQueryService(),
                threadRepositoryPort,
                messageRepositoryPort,
                gmailThreadLockRepositoryPort
        );
        MailAttachmentQueryService mailAttachmentQueryService = new MailAttachmentQueryService(attachmentRepositoryPort);
        return new MailFacade(
                mailAccountQueryService,
                googleAccessTokenEnsureService,
                mailCommandService,
                mailAttachmentQueryService,
                new FakeGoogleMailAttachmentQueryService()
        );
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
        return createMailAccount(user, emailAddress, active, MailProvider.GMAIL);
    }

    private MailAccount createMailAccount(User user, String emailAddress, boolean active, MailProvider provider) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(provider)
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

    private Attachment createAttachment(MailAccount mailAccount, MailProvider provider) {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("gmail-thread-id")
                .direction(Direction.OUTBOUND)
                .read(true)
                .messageCount(1)
                .build();
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message-id")
                .direction(Direction.OUTBOUND)
                .fromAddress(mailAccount.getEmailAddress())
                .read(true)
                .build();
        return Attachment.builder()
                .id(UUID.randomUUID())
                .message(message)
                .gmailAttachmentId(provider == MailProvider.GMAIL ? "gmail-attachment-id" : null)
                .filename("file.txt")
                .mimeType("text/plain")
                .size(12)
                .build();
    }

    private static final class FakeGoogleMailSendCommandService extends GoogleMailSendCommandService {

        private FakeGoogleMailSendCommandService() {
            super(new GoogleMailProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleMailSendResult send(MailAccount mailAccount, MailSendCommand command) {
            return new GoogleMailSendResult("gmail-message-id", "gmail-thread-id");
        }
    }

    private static final class FakeGoogleOAuthQueryService extends GoogleOAuthQueryService {

        private FakeGoogleOAuthQueryService() {
            super(new GoogleOAuthProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleOAuthTokenResult refreshAccessToken(String refreshToken) {
            return new GoogleOAuthTokenResult("refreshed-token", refreshToken, 3600L, null, "Bearer");
        }
    }

    private static final class FakeGoogleMailMessageQueryService extends GoogleMailMessageQueryService {

        private FakeGoogleMailMessageQueryService() {
            super(new GoogleMailProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleMailMessageResult getMessage(String accessToken, String gmailMessageId) {
            return new GoogleMailMessageResult(
                    gmailMessageId,
                    "gmail-thread-id",
                    "history-id",
                    "제목",
                    "sender@example.com",
                    "Sender Name",
                    List.of("to@example.com"),
                    List.of("To Name"),
                    List.of("cc@example.com"),
                    List.of("Cc Name"),
                    "snippet",
                    LocalDateTime.now(),
                    "본문",
                    null,
                    List.of(new GoogleMailAttachmentResult("gmail-attachment-id", "file.txt", "text/plain", 5))
            );
        }
    }

    private static final class FakeGoogleMailAttachmentQueryService extends GoogleMailAttachmentQueryService {

        private FakeGoogleMailAttachmentQueryService() {
            super(new GoogleMailProperties(), RestClient.builder().build());
        }

        @Override
        public byte[] download(MailAccount mailAccount, Message message, Attachment attachment) {
            return "file-content".getBytes();
        }
    }
}
