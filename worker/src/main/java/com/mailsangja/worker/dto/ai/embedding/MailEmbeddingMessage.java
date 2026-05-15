package com.mailsangja.worker.dto.ai.embedding;

import com.mailsangja.worker.common.exception.embedding.EmbeddingErrorCode;
import com.mailsangja.worker.common.exception.embedding.EmbeddingException;

import java.util.UUID;

public record MailEmbeddingMessage(UUID messageId) {

    public MailEmbeddingMessage {
        if (messageId == null) {
            throw new EmbeddingException(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_MESSAGE);
        }
    }
}
