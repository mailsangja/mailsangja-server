package com.mailsangja.worker.service.ai.embedding;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.VectorDocumentRepositoryPort;
import com.mailsangja.worker.common.exception.embedding.EmbeddingErrorCode;
import com.mailsangja.worker.common.exception.embedding.EmbeddingException;
import com.mailsangja.worker.dto.ai.masking.MaskingCommand;
import com.mailsangja.worker.dto.ai.masking.MaskingResult;
import com.mailsangja.worker.service.ai.masking.PhileasMaskingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailEmbeddingCommandService {

    private final MessageRepositoryPort messageRepositoryPort;
    private final VectorDocumentRepositoryPort vectorDocumentRepositoryPort;
    private final MailEmbeddingQueryService mailEmbeddingQueryService;
    private final PhileasMaskingService phileasMaskingService;
    private final VectorStore vectorStore;

    public void embed(UUID messageId) {
        Message message = messageRepositoryPort.findByIdIncludingDeleted(messageId)
                .orElseThrow(() -> new EmbeddingException(EmbeddingErrorCode.MAIL_EMBEDDING_MESSAGE_NOT_FOUND));
        String embeddableText = mailEmbeddingQueryService.extractEmbeddableText(message);
        if (embeddableText.isBlank()) {
            log.debug("Mail embedding skipped because embeddable text is blank. messageId={}", messageId);
            return;
        }

        UUID documentId = mailEmbeddingQueryService.createDocumentId(message);
        MaskingResult maskingResult = phileasMaskingService.mask(embeddableText, MaskingCommand.pastContext());
        List<Document> documents = buildChunkDocuments(message, documentId, maskingResult.maskedText());
        List<Document> missingDocuments = findMissingDocuments(documents);
        if (missingDocuments.isEmpty()) {
            log.debug(
                    "Mail embedding skipped because all documents already exist. messageId={} documentCount={}",
                    messageId,
                    documents.size()
            );
            return;
        }
        vectorStore.add(missingDocuments);
        log.info(
                "Mail embedding completed. messageId={} documentCount={} missingDocumentCount={}",
                messageId,
                documents.size(),
                missingDocuments.size()
        );
    }

    private List<Document> findMissingDocuments(List<Document> documents) {
        List<Document> missingDocuments = new ArrayList<>();
        for (Document document : documents) {
            addIfMissing(missingDocuments, document);
        }
        return missingDocuments;
    }

    private void addIfMissing(List<Document> documents, Document document) {
        UUID documentId = UUID.fromString(document.getId());
        if (!vectorDocumentRepositoryPort.existsByDocumentId(documentId)) {
            documents.add(document);
        }
    }

    private List<Document> buildChunkDocuments(Message message, UUID documentId, String text) {
        List<String> chunks = mailEmbeddingQueryService.splitTextForEmbedding(text);
        List<Document> documents = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            UUID chunkDocumentId = index == 0
                    ? documentId
                    : mailEmbeddingQueryService.createChunkDocumentId(documentId, index);
            documents.add(mailEmbeddingQueryService.buildDocument(
                    message,
                    chunkDocumentId,
                    chunks.get(index),
                    documentId,
                    index,
                    chunks.size()
            ));
        }
        return documents;
    }
}
