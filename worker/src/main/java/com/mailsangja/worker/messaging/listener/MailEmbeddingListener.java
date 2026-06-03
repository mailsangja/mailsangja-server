package com.mailsangja.worker.messaging.listener;

import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import com.mailsangja.worker.service.ai.embedding.MailEmbeddingCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailEmbeddingListener {

    private final MailEmbeddingCommandService mailEmbeddingCommandService;

    @RabbitListener(
            queues = "#{@mailEmbeddingQueue.name}",
            containerFactory = "mailEmbeddingRabbitListenerContainerFactory"
    )
    public void handle(MailEmbeddingMessage message, Message rawMessage) {
        handle(message);
    }

    public void handle(MailEmbeddingMessage message) {
        mailEmbeddingCommandService.embed(message.messageId());
    }
}
