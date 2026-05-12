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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MailEmbeddingIdentityServiceTest {

    private final MailEmbeddingIdentityService service = new MailEmbeddingIdentityService();

    @Test
    void createDocumentId_같은외부메시지면방향이다른메시지도같은문서Id를반환한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        Message inbound = createMessage(mailAccountId, MailProvider.GMAIL, "provider-message-id", Direction.INBOUND);
        Message outbound = createMessage(mailAccountId, MailProvider.GMAIL, "provider-message-id", Direction.OUTBOUND);

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
        Message first = createMessage(UUID.randomUUID(), MailProvider.GMAIL, "provider-message-id", Direction.INBOUND);
        Message second = createMessage(UUID.randomUUID(), MailProvider.GMAIL, "provider-message-id", Direction.INBOUND);

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
        Message first = createMessage(mailAccountId, MailProvider.GMAIL, "provider-message-id-1", Direction.INBOUND);
        Message second = createMessage(mailAccountId, MailProvider.GMAIL, "provider-message-id-2", Direction.INBOUND);

        // when
        UUID firstDocumentId = service.createDocumentId(first);
        UUID secondDocumentId = service.createDocumentId(second);

        // then
        assertNotEquals(firstDocumentId, secondDocumentId);
    }

    private Message createMessage(UUID mailAccountId, MailProvider provider, String providerMessageId, Direction direction) {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(createMailAccount(mailAccountId, provider))
                .gmailThreadId("gmail-thread-id-" + direction)
                .direction(direction)
                .build();

        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(providerMessageId)
                .direction(direction)
                .fromAddress("sender@example.com")
                .bodyText("본문입니다.")
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
