package com.mailsangja.worker.service.ai.embedding;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.VectorDocumentRepositoryPort;
import com.mailsangja.worker.common.exception.embedding.EmbeddingErrorCode;
import com.mailsangja.worker.common.exception.embedding.EmbeddingException;
import com.mailsangja.worker.dto.ai.masking.MaskingCommand;
import com.mailsangja.worker.dto.ai.masking.MaskingResult;
import com.mailsangja.worker.dto.ai.masking.MaskingScope;
import com.mailsangja.worker.service.ai.masking.PhileasMaskingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailEmbeddingCommandServiceTest {

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private VectorDocumentRepositoryPort vectorDocumentRepositoryPort;

    @Mock
    private MailEmbeddingQueryService mailEmbeddingQueryService;

    @Mock
    private PhileasMaskingService phileasMaskingService;

    @Mock
    private VectorStore vectorStore;

    @Test
    void embed_messageId로메시지를찾지못하면커스텀예외를던진다() {
        // given
        UUID messageId = UUID.randomUUID();
        MailEmbeddingCommandService service = createService();
        when(messageRepositoryPort.findByIdIncludingDeleted(messageId)).thenReturn(Optional.empty());

        // when
        EmbeddingException exception = assertThrows(EmbeddingException.class, () -> service.embed(messageId));

        // then
        assertEquals(EmbeddingErrorCode.MAIL_EMBEDDING_MESSAGE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(mailEmbeddingQueryService, vectorDocumentRepositoryPort, phileasMaskingService, vectorStore);
    }

    @Test
    void embed_임베딩가능한본문이blank이면임베딩하지않는다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId, " ", null);
        MailEmbeddingCommandService service = createService();
        when(messageRepositoryPort.findByIdIncludingDeleted(messageId)).thenReturn(Optional.of(message));
        when(mailEmbeddingQueryService.extractEmbeddableText(message)).thenReturn("");

        // when
        service.embed(messageId);

        // then
        verify(mailEmbeddingQueryService).extractEmbeddableText(message);
        verifyNoInteractions(vectorDocumentRepositoryPort, phileasMaskingService, vectorStore);
    }

    @Test
    void embed_vectorStore에이미존재하면본문마스킹과임베딩을하지않는다() {
        // given
        UUID messageId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Message message = createMessage(messageId, "본문입니다.", null);
        MailEmbeddingCommandService service = createService();
        when(messageRepositoryPort.findByIdIncludingDeleted(messageId)).thenReturn(Optional.of(message));
        when(mailEmbeddingQueryService.extractEmbeddableText(message)).thenReturn("본문입니다.");
        when(mailEmbeddingQueryService.createDocumentId(message)).thenReturn(documentId);
        when(vectorDocumentRepositoryPort.existsById(documentId)).thenReturn(true);

        // when
        service.embed(messageId);

        // then
        verify(vectorDocumentRepositoryPort).existsById(documentId);
        verifyNoInteractions(phileasMaskingService, vectorStore);
        verify(mailEmbeddingQueryService, never()).buildDocument(any(), any(), any());
    }

    @Test
    void embed_본문을pastContext로마스킹한뒤VectorStore에저장한다() {
        // given
        UUID messageId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Message message = createMessage(messageId, " 전화번호는 010-1234-5678 입니다. ", null);
        Document document = Document.builder()
                .id(documentId.toString())
                .text("전화번호는 {{PHONE_NUMBER_1}} 입니다.")
                .metadata(Map.of("MessageId", messageId.toString()))
                .build();
        MailEmbeddingCommandService service = createService();
        when(messageRepositoryPort.findByIdIncludingDeleted(messageId)).thenReturn(Optional.of(message));
        when(mailEmbeddingQueryService.extractEmbeddableText(message)).thenReturn("전화번호는 010-1234-5678 입니다.");
        when(mailEmbeddingQueryService.createDocumentId(message)).thenReturn(documentId);
        when(vectorDocumentRepositoryPort.existsById(documentId)).thenReturn(false);
        when(phileasMaskingService.mask(
                eq("전화번호는 010-1234-5678 입니다."),
                argThat(command -> command != null && command.scope() == MaskingScope.PAST_CONTEXT)
        )).thenReturn(maskingResult("전화번호는 {{PHONE_NUMBER_1}} 입니다."));
        when(mailEmbeddingQueryService.buildDocument(message, documentId, "전화번호는 {{PHONE_NUMBER_1}} 입니다."))
                .thenReturn(document);

        // when
        service.embed(messageId);

        // then
        ArgumentCaptor<List<Document>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentsCaptor.capture());
        assertEquals(List.of(document), documentsCaptor.getValue());
    }

    @Test
    void embed_VectorStore저장에실패하면예외를전파한다() {
        // given
        UUID messageId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Message message = createMessage(messageId, "본문입니다.", null);
        Document document = Document.builder()
                .id(documentId.toString())
                .text("마스킹된 본문입니다.")
                .metadata(Map.of("MessageId", messageId.toString()))
                .build();
        RuntimeException vectorStoreFailure = new RuntimeException("vector store failed");
        MailEmbeddingCommandService service = createService();
        when(messageRepositoryPort.findByIdIncludingDeleted(messageId)).thenReturn(Optional.of(message));
        when(mailEmbeddingQueryService.extractEmbeddableText(message)).thenReturn("본문입니다.");
        when(mailEmbeddingQueryService.createDocumentId(message)).thenReturn(documentId);
        when(vectorDocumentRepositoryPort.existsById(documentId)).thenReturn(false);
        when(phileasMaskingService.mask(any(), any(MaskingCommand.class)))
                .thenReturn(maskingResult("마스킹된 본문입니다."));
        when(mailEmbeddingQueryService.buildDocument(message, documentId, "마스킹된 본문입니다.")).thenReturn(document);
        doThrow(vectorStoreFailure).when(vectorStore).add(List.of(document));

        // when
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.embed(messageId));

        // then
        assertEquals(vectorStoreFailure, thrown);
    }

    private MailEmbeddingCommandService createService() {
        return new MailEmbeddingCommandService(
                messageRepositoryPort,
                vectorDocumentRepositoryPort,
                mailEmbeddingQueryService,
                phileasMaskingService,
                vectorStore
        );
    }

    private MaskingResult maskingResult(String maskedText) {
        return new MaskingResult(
                maskedText,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    private Message createMessage(UUID messageId, String bodyText, String bodyHtml) {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(createMailAccount())
                .gmailThreadId("gmail-thread-id")
                .direction(Direction.INBOUND)
                .build();

        return Message.builder()
                .id(messageId)
                .thread(thread)
                .gmailMessageId("provider-message-id")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .toAddresses(List.of("to@example.com"))
                .sentAt(LocalDateTime.of(2026, 5, 4, 10, 0))
                .bodyText(bodyText)
                .bodyHtml(bodyHtml)
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
