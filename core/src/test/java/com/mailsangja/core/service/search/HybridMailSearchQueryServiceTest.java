package com.mailsangja.core.service.search;

import com.mailsangja.core.dto.search.HybridMailSearchScope;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.ContactRepositoryPort;
import com.mailsangja.db.port.MailSearchRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridMailSearchQueryServiceTest {

    @Test
    void vector와lexical후보를Rrf로병합해Message를조회한다() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID vectorFirst = UUID.randomUUID();
        UUID shared = UUID.randomUUID();
        UUID lexicalOnly = UUID.randomUUID();
        MailSearchRepositoryPort repositoryPort = mock(MailSearchRepositoryPort.class);
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        VectorStore vectorStore = mock(VectorStore.class);
        HybridMailSearchQueryService service = new HybridMailSearchQueryService(
                repositoryPort,
                contactRepositoryPort,
                vectorStore,
                new HybridSearchLexicalQueryBuilder()
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                document(vectorFirst),
                document(shared)
        ));
        when(repositoryPort.findHybridLexicalMessageIds(
                eq(userId), eq(accountId), eq(Direction.INBOUND), any(), eq(List.of()), eq(null), any(Integer.class)
        )).thenReturn(List.of(shared, lexicalOnly));
        when(repositoryPort.findHybridMessagesByIds(
                eq(userId), any(), eq(accountId), eq(Direction.INBOUND), eq(List.of()), eq(null)
        )).thenReturn(List.of(
                message(vectorFirst, accountId, Direction.INBOUND),
                message(shared, accountId, Direction.INBOUND),
                message(lexicalOnly, accountId, Direction.INBOUND)
        ));
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(eq(userId), any())).thenReturn(List.of());

        var result = service.search(userId, "프로젝트 일정", HybridMailSearchScope.INBOX, accountId, List.of(), null, 10);

        assertEquals(List.of(shared, vectorFirst, lexicalOnly), result.items().stream()
                .map(item -> item.message().getId())
                .toList());
        verify(repositoryPort).findHybridMessagesByIds(
                eq(userId),
                eq(List.of(shared, vectorFirst, lexicalOnly)),
                eq(accountId),
                eq(Direction.INBOUND),
                eq(List.of()),
                eq(null)
        );
    }

    @Test
    void lexical만성공해도검색결과를반환한다() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MailSearchRepositoryPort repositoryPort = mock(MailSearchRepositoryPort.class);
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        VectorStore vectorStore = mock(VectorStore.class);
        HybridMailSearchQueryService service = new HybridMailSearchQueryService(
                repositoryPort,
                contactRepositoryPort,
                vectorStore,
                new HybridSearchLexicalQueryBuilder()
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("vector failed"));
        when(repositoryPort.findHybridLexicalMessageIds(eq(userId), eq(null), eq(null), any(), eq(null), eq(Boolean.FALSE), any(Integer.class)))
                .thenReturn(List.of(messageId));
        when(repositoryPort.findHybridMessagesByIds(eq(userId), eq(List.of(messageId)), eq(null), eq(null), eq(null), eq(Boolean.FALSE)))
                .thenReturn(List.of(message(messageId, UUID.randomUUID(), Direction.OUTBOUND)));
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(eq(userId), any())).thenReturn(List.of());

        var result = service.search(userId, "회의", HybridMailSearchScope.ALL, null, null, Boolean.FALSE, 5);

        assertEquals(List.of(messageId), result.items().stream()
                .map(item -> item.message().getId())
                .toList());
    }

    private Document document(UUID messageId) {
        return new Document(messageId.toString(), "text", Map.of("MessageId", messageId.toString()));
    }

    private Message message(UUID messageId, UUID accountId, Direction direction) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .username("alice")
                .password("password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .build();
        MailAccount mailAccount = MailAccount.builder()
                .id(accountId)
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress("alice@example.com")
                .alias("Alice")
                .active(true)
                .build();
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("gmail-thread")
                .direction(direction)
                .messageCount(1)
                .read(false)
                .build();
        return Message.builder()
                .id(messageId)
                .thread(thread)
                .gmailMessageId("gmail-message")
                .direction(direction)
                .subject("subject")
                .fromAddress("sender@example.com")
                .toAddresses(List.of("alice@example.com"))
                .read(false)
                .build();
    }
}
