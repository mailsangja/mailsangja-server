package com.mailsangja.worker.dto.ai.embedding;

import com.mailsangja.worker.common.exception.embedding.EmbeddingErrorCode;
import com.mailsangja.worker.common.exception.embedding.EmbeddingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailEmbeddingMessageTest {

    @Test
    void 생성자_messageId가null이면커스텀예외를던진다() {
        // when
        EmbeddingException exception = assertThrows(EmbeddingException.class, () -> new MailEmbeddingMessage(null));

        // then
        assertEquals(EmbeddingErrorCode.INVALID_MAIL_EMBEDDING_MESSAGE, exception.getErrorCode());
    }

    @Test
    void 생성자_messageId를보관한다() {
        // given
        UUID messageId = UUID.randomUUID();

        // when
        MailEmbeddingMessage message = new MailEmbeddingMessage(messageId);

        // then
        assertEquals(messageId, message.messageId());
    }
}
