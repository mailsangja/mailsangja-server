package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.dto.MailDraftReferenceMessageResult;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.module.mail.MessageJpaRepositoryModule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailDraftReferenceQueryAdapterTest {

    @Test
    void 최근작성메일은요청계정Id로결과를만들어ThreadLazy로딩을피한다() {
        // given
        UUID messageId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        Message message = createMessageWithoutThread(messageId);
        MailDraftReferenceQueryAdapter adapter = createRecentAdapter(message);

        // when
        List<MailDraftReferenceMessageResult> results = adapter.findRecentWrittenMessages(
                UUID.randomUUID(), mailAccountId, 4
        );

        // then
        assertEquals(mailAccountId, results.getFirst().mailAccountId());
    }

    @Test
    void 수신자히스토리메일은요청계정Id로결과를만든다() {
        // given
        UUID messageId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        Message message = createMessageWithoutThread(messageId);
        MailDraftReferenceQueryAdapter adapter = createRecipientHistoryAdapter(message);

        // when
        List<MailDraftReferenceMessageResult> results = adapter.findRecipientHistoryMessages(
                UUID.randomUUID(), mailAccountId, List.of("kim@example.com"), 6
        );

        // then
        assertEquals(mailAccountId, results.getFirst().mailAccountId());
    }

    @Test
    void bodyText가없으면Html을텍스트로변환한다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createHtmlMessage(messageId);
        MailDraftReferenceQueryAdapter adapter = createAdapter(message);

        // when
        List<MailDraftReferenceMessageResult> results = adapter.findMessagesByIds(List.of(messageId));

        // then
        assertEquals("문의 user@example.com\n감사합니다.", results.getFirst().body());
    }

    @Test
    void 계정Lexical검색결과Id를Uuid로변환한다() {
        // given
        UUID messageId = UUID.randomUUID();
        MailDraftReferenceQueryAdapter adapter = createLexicalAdapter(messageId);

        // when
        List<UUID> results = adapter.findAccountLexicalRelevantMessageIds(
                UUID.randomUUID(), UUID.randomUUID(), "수강 | 정정", 40
        );

        // then
        assertEquals(List.of(messageId), results);
    }

    @Test
    void 사용자Lexical검색결과Id를Uuid로변환한다() {
        // given
        UUID messageId = UUID.randomUUID();
        MailDraftReferenceQueryAdapter adapter = createLexicalAdapter(messageId);

        // when
        List<UUID> results = adapter.findUserLexicalRelevantMessageIds(
                UUID.randomUUID(), "수강 | 정정", 40
        );

        // then
        assertEquals(List.of(messageId), results);
    }

    private MailDraftReferenceQueryAdapter createAdapter(Message message) {
        MessageJpaRepositoryModule module = (MessageJpaRepositoryModule) Proxy.newProxyInstance(
                MessageJpaRepositoryModule.class.getClassLoader(),
                new Class<?>[]{MessageJpaRepositoryModule.class},
                (proxy, method, args) -> findActiveByIdIn(method.getName(), message)
        );
        return new MailDraftReferenceQueryAdapter(module);
    }

    private MailDraftReferenceQueryAdapter createRecentAdapter(Message message) {
        MessageJpaRepositoryModule module = (MessageJpaRepositoryModule) Proxy.newProxyInstance(
                MessageJpaRepositoryModule.class.getClassLoader(),
                new Class<?>[]{MessageJpaRepositoryModule.class},
                (proxy, method, args) -> findRecentByUserIdAndMailAccountIdAndDirection(method.getName(), message)
        );
        return new MailDraftReferenceQueryAdapter(module);
    }

    private MailDraftReferenceQueryAdapter createRecipientHistoryAdapter(Message message) {
        MessageJpaRepositoryModule module = (MessageJpaRepositoryModule) Proxy.newProxyInstance(
                MessageJpaRepositoryModule.class.getClassLoader(),
                new Class<?>[]{MessageJpaRepositoryModule.class},
                (proxy, method, args) -> findRecipientHistoryByUserIdAndMailAccountIdAndHint(method.getName(), message)
        );
        return new MailDraftReferenceQueryAdapter(module);
    }

    private MailDraftReferenceQueryAdapter createLexicalAdapter(UUID messageId) {
        MessageJpaRepositoryModule module = (MessageJpaRepositoryModule) Proxy.newProxyInstance(
                MessageJpaRepositoryModule.class.getClassLoader(),
                new Class<?>[]{MessageJpaRepositoryModule.class},
                (proxy, method, args) -> findLexicalMessageIds(method.getName(), messageId)
        );
        return new MailDraftReferenceQueryAdapter(module);
    }

    private Object findRecentByUserIdAndMailAccountIdAndDirection(String methodName, Message message) {
        if ("findRecentByUserIdAndMailAccountIdAndDirection".equals(methodName)) {
            return List.of(message);
        }
        throw new UnsupportedOperationException(methodName);
    }

    private Object findRecipientHistoryByUserIdAndMailAccountIdAndHint(String methodName, Message message) {
        if ("findRecipientHistoryByUserIdAndMailAccountIdAndHint".equals(methodName)) {
            return List.of(message);
        }
        throw new UnsupportedOperationException(methodName);
    }

    private Object findActiveByIdIn(String methodName, Message message) {
        if ("findActiveByIdIn".equals(methodName)) {
            return List.of(message);
        }
        throw new UnsupportedOperationException(methodName);
    }

    private Object findLexicalMessageIds(String methodName, UUID messageId) {
        if ("findAccountLexicalRelevantMessageIds".equals(methodName)
                || "findUserLexicalRelevantMessageIds".equals(methodName)) {
            return List.of(messageId.toString());
        }
        throw new UnsupportedOperationException(methodName);
    }

    private Message createHtmlMessage(UUID messageId) {
        return Message.builder()
                .id(messageId)
                .thread(createThread())
                .gmailMessageId("gmail-message-id")
                .direction(Direction.OUTBOUND)
                .subject("subject")
                .fromAddress("from@example.com")
                .bodyHtml("<p>문의 <span>user</span>@example.com</p><p>감사합니다.</p>")
                .build();
    }

    private Message createMessageWithoutThread(UUID messageId) {
        return Message.builder()
                .id(messageId)
                .gmailMessageId("gmail-message-id")
                .direction(Direction.OUTBOUND)
                .subject("subject")
                .fromAddress("from@example.com")
                .bodyText("body")
                .build();
    }

    private Thread createThread() {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(MailAccount.builder().id(UUID.randomUUID()).build())
                .gmailThreadId("gmail-thread-id")
                .direction(Direction.OUTBOUND)
                .read(true)
                .messageCount(1)
                .build();
    }
}
