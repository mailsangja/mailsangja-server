package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.dto.mail.GoogleMailAttachmentResult;
import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
import com.mailsangja.core.dto.mail.GoogleMailSendResult;
import com.mailsangja.core.dto.mail.MailAttachmentDownloadResult;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.service.google.GoogleMailAttachmentQueryService;
import com.mailsangja.core.service.google.GoogleMailMessageQueryService;
import com.mailsangja.core.service.google.GoogleMailSendCommandService;
import com.mailsangja.core.service.mail.MailAttachmentQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.mail.MailCommandService;
import com.mailsangja.core.service.mail.MailQueryService;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.GmailThreadLock;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailFacadeTest {

    @Test
    void sendMail_내활성메일계정이면검증을통과한다() {
        User user = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount));

        MailSendRequest request = new MailSendRequest(
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
    void sendMail_subject와content가둘다비어있으면실패한다() {
        User user = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount));

        MailSendRequest request = new MailSendRequest(
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
    void sendMail_subject에개행문자가있으면실패한다() {
        User user = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount));

        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                List.of("to@example.com"),
                null,
                null,
                "제목\n추가",
                "본문",
                null
        );

        assertThrows(MailSendException.class, () -> mailFacade.sendMail(user, request));
    }

    @Test
    void sendMail_중복된수신자가있으면실패한다() {
        User user = createUser(UUID.randomUUID());
        MailFacade mailFacade = createMailFacade(List.of());

        MailSendRequest request = new MailSendRequest(
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
    void sendMail_to가비어있고cc만있어도실패한다() {
        User user = createUser(UUID.randomUUID());
        MailFacade mailFacade = createMailFacade(List.of());

        MailSendRequest request = new MailSendRequest(
                "sender@example.com",
                List.of(),
                List.of("cc@example.com"),
                null,
                "제목",
                "본문",
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

    @Test
    void getAttachment_내활성메일계정의지메일첨부파일이면다운로드한다() {
        User user = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(user, "sender@example.com", true);
        Attachment attachment = createAttachment(mailAccount, MailProvider.GMAIL);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount), List.of(attachment));

        MailAttachmentDownloadResult result = mailFacade.getAttachment(user, attachment.getId());

        org.junit.jupiter.api.Assertions.assertArrayEquals("file-content".getBytes(), result.bytes());
        org.junit.jupiter.api.Assertions.assertEquals("text/plain", result.mimeType());
    }

    @Test
    void getAttachment_다른사용자계정의첨부파일이면실패한다() {
        User owner = createUser(UUID.randomUUID());
        User anotherUser = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(owner, "sender@example.com", true);
        Attachment attachment = createAttachment(mailAccount, MailProvider.GMAIL);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount), List.of(attachment));

        assertThrows(InboxException.class, () -> mailFacade.getAttachment(anotherUser, attachment.getId()));
    }

    @Test
    void getAttachment_지메일이아닌첨부파일이면실패한다() {
        User user = createUser(UUID.randomUUID());
        MailAccount mailAccount = createMailAccount(user, "sender@example.com", true, MailProvider.NAVER);
        Attachment attachment = createAttachment(mailAccount, MailProvider.NAVER);
        MailFacade mailFacade = createMailFacade(List.of(mailAccount), List.of(attachment));

        assertThrows(InboxException.class, () -> mailFacade.getAttachment(user, attachment.getId()));
    }

    private MailFacade createMailFacade(List<MailAccount> mailAccounts) {
        return createMailFacade(mailAccounts, List.of());
    }

    private MailFacade createMailFacade(List<MailAccount> mailAccounts, List<Attachment> attachments) {
        MailQueryService mailQueryService = new MailQueryService(new FakeMailAccountRepositoryPort(mailAccounts));
        MailAccountQueryService mailAccountQueryService = new MailAccountQueryService(new FakeMailAccountRepositoryPort(mailAccounts));
        MailCommandService mailCommandService = new MailCommandService(
                mailQueryService,
                new FakeGoogleMailSendCommandService(),
                new FakeGoogleMailMessageQueryService(),
                new FakeThreadRepositoryPort(),
                new FakeMessageRepositoryPort(),
                new FakeGmailThreadLockRepositoryPort()
        );
        MailAttachmentQueryService mailAttachmentQueryService = new MailAttachmentQueryService(
                new FakeAttachmentRepositoryPort(attachments)
        );
        return new MailFacade(
                mailAccountQueryService,
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
            return mailAccounts.stream()
                    .filter(mailAccount -> mailAccount.getUser() != null)
                    .filter(mailAccount -> userId.equals(mailAccount.getUser().getId()))
                    .filter(mailAccount -> mailAccount.isActive() == active)
                    .toList();
        }
    }

    private static class FakeAttachmentRepositoryPort implements AttachmentRepositoryPort {

        private final List<Attachment> attachments;

        private FakeAttachmentRepositoryPort(List<Attachment> attachments) {
            this.attachments = attachments;
        }

        @Override
        public Attachment save(Attachment attachment) {
            return attachment;
        }

        @Override
        public Optional<Attachment> findByIdAndDeletedAtIsNull(UUID id) {
            return attachments.stream()
                    .filter(attachment -> id.equals(attachment.getId()))
                    .findFirst();
        }

        @Override
        public List<Attachment> findAllByMessageIdAndDeletedAtIsNull(UUID messageId) {
            return List.of();
        }
    }

    private static class FakeGoogleMailSendCommandService extends GoogleMailSendCommandService {

        private FakeGoogleMailSendCommandService() {
            super(new com.mailsangja.core.config.properties.GoogleMailProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleMailSendResult send(MailAccount mailAccount, com.mailsangja.core.dto.mail.MailSendCommand command) {
            return new GoogleMailSendResult(
                    "gmail-message-id",
                    "gmail-thread-id"
            );
        }
    }

    private static class FakeGoogleMailMessageQueryService extends GoogleMailMessageQueryService {

        private FakeGoogleMailMessageQueryService() {
            super(new com.mailsangja.core.config.properties.GoogleMailProperties(), RestClient.builder().build());
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
                    List.of(new GoogleMailAttachmentResult(
                            "gmail-attachment-id",
                            "file.txt",
                            "text/plain",
                            5
                    ))
            );
        }
    }

    private static class FakeGoogleMailAttachmentQueryService extends GoogleMailAttachmentQueryService {

        private FakeGoogleMailAttachmentQueryService() {
            super(new com.mailsangja.core.config.properties.GoogleMailProperties(), RestClient.builder().build());
        }

        @Override
        public byte[] download(MailAccount mailAccount, Message message, Attachment attachment) {
            return "file-content".getBytes();
        }
    }

    private static class FakeThreadRepositoryPort implements ThreadRepositoryPort {

        private final List<Thread> threads = new ArrayList<>();

        @Override
        public Thread save(Thread thread) {
            Thread savedThread = thread.getId() == null
                    ? Thread.builder()
                    .id(UUID.randomUUID())
                    .mailAccount(thread.getMailAccount())
                    .gmailThreadId(thread.getGmailThreadId())
                    .direction(thread.getDirection())
                    .historyId(thread.getHistoryId())
                    .latestSubject(thread.getLatestSubject())
                    .latestSnippet(thread.getLatestSnippet())
                    .latestParticipantAddress(thread.getLatestParticipantAddress())
                    .lastMessageAt(thread.getLastMessageAt())
                    .read(thread.isRead())
                    .messageCount(thread.getMessageCount())
                    .build()
                    : thread;
            threads.removeIf(existing -> existing.getId() != null && existing.getId().equals(savedThread.getId()));
            threads.add(savedThread);
            return savedThread;
        }

        @Override
        public Optional<Thread> findByIdAndDeletedAtIsNull(UUID id) {
            return threads.stream().filter(thread -> id.equals(thread.getId())).findFirst();
        }

        public Optional<Thread> findByIdIncludingDeleted(UUID id) {
            return threads.stream().filter(thread -> id.equals(thread.getId())).findFirst();
        }

        @Override
        public Optional<Thread> findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                UUID mailAccountId,
                String gmailThreadId,
                Direction direction
        ) {
            return threads.stream()
                    .filter(thread -> thread.getMailAccount() != null)
                    .filter(thread -> mailAccountId.equals(thread.getMailAccount().getId()))
                    .filter(thread -> gmailThreadId.equals(thread.getGmailThreadId()))
                    .filter(thread -> direction == thread.getDirection())
                    .findFirst();
        }

        @Override
        public List<Thread> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
            return List.of();
        }

        public List<Thread> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return List.of();
        }

        public void hardDeleteAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
        }

        public int bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return 0;
        }

        public int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return 0;
        }

        public int bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                UUID mailAccountId,
                String gmailThreadId,
                LocalDateTime deletedAt
        ) {
            return 0;
        }

        @Override
        public org.springframework.data.domain.Slice<Thread> findInboxByUserIdAndDeletedAtIsNull(
                UUID userId,
                UUID markerId,
                org.springframework.data.domain.Pageable pageable
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.springframework.data.domain.Slice<Thread> findSentByUserIdAndDeletedAtIsNull(
                UUID userId,
                UUID markerId,
                org.springframework.data.domain.Pageable pageable
        ) {
            throw new UnsupportedOperationException();
        }

        public Slice<Thread> findTrashByUserId(UUID userId, UUID markerId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countUnreadInboxByUserId(UUID userId) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeGmailThreadLockRepositoryPort implements GmailThreadLockRepositoryPort {

        @Override
        public GmailThreadLock save(GmailThreadLock gmailThreadLock) {
            return gmailThreadLock;
        }

        @Override
        public void acquireThreadLock(MailAccount mailAccount, String gmailThreadId) {
        }

        @Override
        public Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                UUID mailAccountId,
                String gmailThreadId
        ) {
            return Optional.empty();
        }

        @Override
        public Optional<GmailThreadLock> findByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullForUpdate(
                UUID mailAccountId,
                String gmailThreadId
        ) {
            return Optional.empty();
        }
    }

    private static class FakeMessageRepositoryPort implements MessageRepositoryPort {

        private final List<Message> messages = new ArrayList<>();

        public Optional<Message> findByIdIncludingDeleted(UUID id) {
            return messages.stream()
                    .filter(message -> id.equals(message.getId()))
                    .findFirst();
        }

        @Override
        public Message save(Message message) {
            messages.removeIf(existing -> existing.getId() != null && existing.getId().equals(message.getId()));
            messages.add(message);
            return message;
        }

        @Override
        public Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId) {
            if (threadId == null) {
                return Optional.empty();
            }
            return messages.stream()
                    .filter(message -> message.getThread() != null)
                    .filter(message -> threadId.equals(message.getThread().getId()))
                    .filter(message -> gmailMessageId.equals(message.getGmailMessageId()))
                    .findFirst();
        }

        public Optional<Message> findByThreadIdAndGmailMessageId(UUID threadId, String gmailMessageId) {
            if (threadId == null) {
                return Optional.empty();
            }
            return messages.stream()
                    .filter(message -> message.getThread() != null)
                    .filter(message -> threadId.equals(message.getThread().getId()))
                    .filter(message -> gmailMessageId.equals(message.getGmailMessageId()))
                    .findFirst();
        }

        @Override
        public Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                UUID mailAccountId,
                String gmailThreadId,
                String gmailMessageId
        ) {
            return Optional.empty();
        }

        public Optional<Message> findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                UUID mailAccountId,
                String gmailThreadId,
                String gmailMessageId
        ) {
            return Optional.empty();
        }

        @Override
        public List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId) {
            return List.of();
        }

        public List<Message> findAllByThreadIdIncludingDeleted(UUID threadId) {
            return List.of();
        }

        @Override
        public List<Message> findAllByThreadIdInAndDeletedAtIsNull(List<UUID> threadIds) {
            return List.of();
        }

        @Override
        public List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
            return List.of();
        }

        public List<Message> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return List.of();
        }

        public boolean existsByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return messages.stream()
                    .filter(message -> message.getThread() != null)
                    .filter(message -> message.getThread().getMailAccount() != null)
                    .anyMatch(message -> mailAccountId.equals(message.getThread().getMailAccount().getId())
                            && gmailThreadId.equals(message.getThread().getGmailThreadId()));
        }

        public boolean existsByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndGmailMessageIdNot(
                UUID mailAccountId,
                String gmailThreadId,
                String gmailMessageId
        ) {
            return messages.stream()
                    .filter(message -> message.getThread() != null)
                    .filter(message -> message.getThread().getMailAccount() != null)
                    .anyMatch(message -> mailAccountId.equals(message.getThread().getMailAccount().getId())
                            && gmailThreadId.equals(message.getThread().getGmailThreadId())
                            && !gmailMessageId.equals(message.getGmailMessageId()));
        }

        public int bulkRestoreByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return 0;
        }

        public int bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                UUID mailAccountId,
                String gmailThreadId,
                LocalDateTime deletedAt
        ) {
            return 0;
        }

        public List<Message> findAllDeletedByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId) {
            return List.of();
        }

        public Slice<Message> findDeletedByUserId(UUID userId, UUID markerId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        public void hardDelete(Message message) {
            messages.remove(message);
        }
    }
}
