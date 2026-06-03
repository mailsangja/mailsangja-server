package com.mailsangja.worker.messaging.listener;

import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionMessage;
import com.mailsangja.worker.service.mail.ReplyDraftSuggestionCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyDraftSuggestionListener {

    private final ReplyDraftSuggestionCommandService replyDraftSuggestionCommandService;

    @RabbitListener(
            queues = "#{@replyDraftSuggestionQueue.name}",
            containerFactory = "replyDraftSuggestionRabbitListenerContainerFactory"
    )
    public void handle(ReplyDraftSuggestionMessage message, Message rawMessage) {
        handle(message);
    }

    public void handle(ReplyDraftSuggestionMessage message) {
        if (message == null) return;
        log.info("Reply draft suggestion message received. messageId={}", message.messageId());
        replyDraftSuggestionCommandService.generate(message.messageId());
    }
}
