package com.mailsangja.worker.messaging.listener;

import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplyDraftSuggestionListener {

    @RabbitListener(
            queues = "#{@replyDraftSuggestionQueue.name}",
            containerFactory = "replyDraftSuggestionRabbitListenerContainerFactory"
    )
    public void handle(ReplyDraftSuggestionMessage message) {
        log.info("Reply draft suggestion message received. messageId={}", message.messageId());
    }
}
