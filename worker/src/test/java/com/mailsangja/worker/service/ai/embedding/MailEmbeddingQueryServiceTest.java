package com.mailsangja.worker.service.ai.embedding;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.worker.common.exception.embedding.EmbeddingErrorCode;
import com.mailsangja.worker.common.exception.embedding.EmbeddingException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailEmbeddingQueryServiceTest {

    private final MailEmbeddingQueryService service = new MailEmbeddingQueryService();

    @Test
    void extractEmbeddableText_bodyText가있으면공백을정리한본문을반환한다() {
        // given
        Message message = createMessage(UUID.randomUUID(), UUID.randomUUID(), MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, "  첫 줄\r\n\r\n  둘째   줄  ", null);

        // when
        String embeddableText = service.extractEmbeddableText(message);

        // then
        assertEquals("첫 줄\n둘째 줄", embeddableText);
    }

    @Test
    void extractEmbeddableText_bodyText가blank이고bodyHtml이있으면html을텍스트로변환한다() {
        // given
        Message message = createMessage(UUID.randomUUID(), UUID.randomUUID(), MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, " ",
                """
                        <html>
                          <head>
                            <style>.hidden { color: red; }</style>
                            <script>alert('x');</script>
                          </head>
                          <body>
                            <p>안녕하세요&nbsp;<strong>메일상자</strong>입니다.</p>
                            <div>확인 부탁드립니다.<br>감사합니다.</div>
                          </body>
                        </html>
                        """);

        // when
        String embeddableText = service.extractEmbeddableText(message);

        // then
        assertEquals("안녕하세요 메일상자입니다.\n확인 부탁드립니다.\n감사합니다.", embeddableText);
    }

    @Test
    void extractEmbeddableText_bodyText와bodyHtml이모두blank이면빈문자열을반환한다() {
        // given
        Message message = createMessage(UUID.randomUUID(), UUID.randomUUID(), MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, " ", " ");

        // when
        String embeddableText = service.extractEmbeddableText(message);

        // then
        assertEquals("", embeddableText);
    }

    @Test
    void createDocumentId_같은외부메시지면방향이다른메시지도같은문서Id를반환한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        Message inbound = createMessage(UUID.randomUUID(), mailAccountId, MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, "본문입니다.", null);
        Message outbound = createMessage(UUID.randomUUID(), mailAccountId, MailProvider.GMAIL,
                "provider-message-id", Direction.OUTBOUND, "본문입니다.", null);

        // when
        UUID inboundDocumentId = service.createDocumentId(inbound);
        UUID outboundDocumentId = service.createDocumentId(outbound);

        // then
        assertEquals(inboundDocumentId, outboundDocumentId);
        assertNotEquals(inbound.getId(), inboundDocumentId);
        assertNotEquals(outbound.getId(), outboundDocumentId);
    }

    @Test
    void createDocumentId_메일계정이다른같은외부메시지도같은문서Id를반환한다() {
        // given
        Message first = createMessage(UUID.randomUUID(), UUID.randomUUID(), MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, "본문입니다.", null);
        Message second = createMessage(UUID.randomUUID(), UUID.randomUUID(), MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, "본문입니다.", null);

        // when
        UUID firstDocumentId = service.createDocumentId(first);
        UUID secondDocumentId = service.createDocumentId(second);

        // then
        assertEquals(firstDocumentId, secondDocumentId);
    }

    @Test
    void createDocumentId_외부메시지Id가다르면다른문서Id를반환한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        Message first = createMessage(UUID.randomUUID(), mailAccountId, MailProvider.GMAIL,
                "provider-message-id-1", Direction.INBOUND, "본문입니다.", null);
        Message second = createMessage(UUID.randomUUID(), mailAccountId, MailProvider.GMAIL,
                "provider-message-id-2", Direction.INBOUND, "본문입니다.", null);

        // when
        UUID firstDocumentId = service.createDocumentId(first);
        UUID secondDocumentId = service.createDocumentId(second);

        // then
        assertNotEquals(firstDocumentId, secondDocumentId);
    }

    @Test
    void createDocumentId_message가null이면커스텀예외를던진다() {
        // when
        EmbeddingException exception = assertThrows(EmbeddingException.class, () -> service.createDocumentId(null));

        // then
        assertEquals(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_MESSAGE, exception.getErrorCode());
    }

    @Test
    void createDocumentId_외부메시지Id가blank이면커스텀예외를던진다() {
        // given
        Message message = createMessage(UUID.randomUUID(), UUID.randomUUID(), MailProvider.GMAIL,
                " ", Direction.INBOUND, "본문입니다.", null);

        // when
        EmbeddingException exception = assertThrows(EmbeddingException.class, () -> service.createDocumentId(message));

        // then
        assertEquals(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_MESSAGE, exception.getErrorCode());
    }

    @Test
    void buildDocument_계산된문서Id와마스킹된본문그리고필수metadata로Document를생성한다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId, UUID.randomUUID(), MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, "본문입니다.", null);
        UUID documentId = UUID.randomUUID();

        // when
        Document document = service.buildDocument(message, documentId, "마스킹된 본문입니다.");
        Map<String, Object> metadata = document.getMetadata();

        // then
        assertEquals(documentId.toString(), document.getId());
        assertEquals("마스킹된 본문입니다.", document.getText());
        assertRequiredMetadata(message, metadata);
    }

    @Test
    void buildDocument_documentId가null이면커스텀예외를던진다() {
        // given
        Message message = createMessage(UUID.randomUUID(), UUID.randomUUID(), MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, "본문입니다.", null);

        // when
        EmbeddingException exception = assertThrows(
                EmbeddingException.class,
                () -> service.buildDocument(message, null, "마스킹된 본문입니다.")
        );

        // then
        assertEquals(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_DOCUMENT, exception.getErrorCode());
    }

    @Test
    void buildDocument_마스킹본문이blank이면커스텀예외를던진다() {
        // given
        Message message = createMessage(UUID.randomUUID(), UUID.randomUUID(), MailProvider.GMAIL,
                "provider-message-id", Direction.INBOUND, "본문입니다.", null);

        // when
        EmbeddingException exception = assertThrows(
                EmbeddingException.class,
                () -> service.buildDocument(message, UUID.randomUUID(), " ")
        );

        // then
        assertEquals(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_DOCUMENT, exception.getErrorCode());
    }

    private void assertRequiredMetadata(Message message, Map<String, Object> metadata) {
        Thread thread = message.getThread();
        MailAccount mailAccount = thread.getMailAccount();
        assertEquals(mailAccount.getUser().getId().toString(), metadata.get("UserId"));
        assertEquals(mailAccount.getId().toString(), metadata.get("MailAccountId"));
        assertEquals(message.getId().toString(), metadata.get("MessageId"));
        assertEquals(thread.getId().toString(), metadata.get("ThreadId"));
        assertEquals(message.getSentAt().toString(), metadata.get("ReceivedAt"));
        assertEquals("sender@example.com", metadata.get("FromMailAddress"));
        assertEquals(List.of("to@example.com"), metadata.get("ToMailAddress"));
    }

    private Message createMessage(
            UUID messageId,
            UUID mailAccountId,
            MailProvider provider,
            String providerMessageId,
            Direction direction,
            String bodyText,
            String bodyHtml
    ) {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(createMailAccount(mailAccountId, provider))
                .gmailThreadId("gmail-thread-id-" + direction)
                .direction(direction)
                .build();

        return Message.builder()
                .id(messageId)
                .thread(thread)
                .gmailMessageId(providerMessageId)
                .direction(direction)
                .fromAddress("sender@example.com")
                .toAddresses(List.of("to@example.com"))
                .sentAt(LocalDateTime.of(2026, 5, 4, 10, 0))
                .bodyText(bodyText)
                .bodyHtml(bodyHtml)
                .build();
    }

    private MailAccount createMailAccount(UUID mailAccountId, MailProvider provider) {
        return MailAccount.builder()
                .id(mailAccountId)
                .user(createUser())
                .provider(provider)
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
