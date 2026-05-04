package com.mailsangja.worker.service.ai.embedding;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailEmbeddingDocumentServiceTest {

    private final MailEmbeddingDocumentService service = new MailEmbeddingDocumentService();

    @Test
    void hasBodyText_returnsFalseForBlankBodyText() {
        Message message = createMessage(" ");

        assertFalse(service.hasBodyText(message));
    }

    @Test
    void build_usesMessageIdAndRequiredMetadata() {
        Message message = createMessage("본문입니다.");

        Document document = service.build(message);
        Map<String, Object> metadata = document.getMetadata();

        assertEquals(message.getId().toString(), document.getId());
        assertEquals("본문입니다.", document.getText());
        assertRequiredMetadata(message, metadata);
    }

    private void assertRequiredMetadata(Message message, Map<String, Object> metadata) {
        Thread thread = message.getThread();
        MailAccount mailAccount = thread.getMailAccount();
        assertEquals(mailAccount.getUser().getId().toString(), metadata.get("UserId"));
        assertEquals(mailAccount.getId().toString(), metadata.get("MailAccountId"));
        assertEquals(message.getId().toString(), metadata.get("MessageId"));
        assertEquals(thread.getId().toString(), metadata.get("ThreadId"));
        assertAddressMetadata(message, metadata);
    }

    private void assertAddressMetadata(Message message, Map<String, Object> metadata) {
        assertEquals(message.getSentAt().toString(), metadata.get("ReceivedAt"));
        assertEquals("sender@example.com", metadata.get("FromMailAddress"));
        assertEquals(List.of("to@example.com"), metadata.get("ToMailAddress"));
    }

    private Message createMessage(String bodyText) {
        Thread thread = createThread();
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message-id")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .toAddresses(List.of("to@example.com"))
                .sentAt(LocalDateTime.of(2026, 5, 4, 10, 0))
                .bodyText(bodyText)
                .build();
    }

    private Thread createThread() {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(createMailAccount())
                .gmailThreadId("gmail-thread-id")
                .direction(Direction.INBOUND)
                .build();
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(createUser())
                .provider(MailProvider.GMAIL)
                .emailAddress("me@example.com")
                .build();
    }

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("사용자")
                .username("user@example.com")
                .password("password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .build();
    }
}
