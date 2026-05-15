package com.mailsangja.worker.messaging.listener;

import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import com.mailsangja.worker.service.ai.embedding.MailEmbeddingCommandService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MailEmbeddingListenerTest {

    @Test
    void handle_messageId로임베딩CommandService를호출한다() {
        // given
        MailEmbeddingCommandService mailEmbeddingCommandService = mock(MailEmbeddingCommandService.class);
        MailEmbeddingListener listener = new MailEmbeddingListener(mailEmbeddingCommandService);
        UUID messageId = UUID.randomUUID();
        MailEmbeddingMessage message = new MailEmbeddingMessage(messageId);

        // when
        listener.handle(message);

        // then
        verify(mailEmbeddingCommandService).embed(messageId);
    }
}
