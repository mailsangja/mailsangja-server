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
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
            return;
        }

        UUID documentId = mailEmbeddingQueryService.createDocumentId(message);
        if (vectorDocumentRepositoryPort.existsById(documentId)) {
            return;
        }

        MaskingResult maskingResult = phileasMaskingService.mask(embeddableText, MaskingCommand.pastContext());
        Document document = mailEmbeddingQueryService.buildDocument(message, documentId, maskingResult.maskedText());
        vectorStore.add(List.of(document));
    }
}
