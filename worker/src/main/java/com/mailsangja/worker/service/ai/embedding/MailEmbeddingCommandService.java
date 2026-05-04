package com.mailsangja.worker.service.ai.embedding;

import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.VectorDocumentRepositoryPort;
import com.mailsangja.worker.service.ai.masking.PhileasMaskingService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MailEmbeddingCommandService {

    public MailEmbeddingCommandService(
            MessageRepositoryPort messageRepositoryPort,
            VectorDocumentRepositoryPort vectorDocumentRepositoryPort,
            MailEmbeddingIdentityService mailEmbeddingIdentityService,
            MailEmbeddingDocumentService mailEmbeddingDocumentService,
            PhileasMaskingService phileasMaskingService,
            VectorStore vectorStore
    ) {
    }

    public void embed(UUID messageId) {
    }
}
