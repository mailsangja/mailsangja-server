package com.mailsangja.worker.messaging.publisher;

import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.ReplyDraftSuggestionRabbitProperties;
import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyDraftSuggestionPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final ReplyDraftSuggestionRabbitProperties replyDraftSuggestionRabbitProperties;

    public void publish(ReplyDraftSuggestionMessage message) {
        rabbitTemplate.convertAndSend(
                mailTaskRabbitProperties.getExchange(),
                replyDraftSuggestionRabbitProperties.getRoutingKey(),
                message,
                new CorrelationData(message.messageId().toString())
        );
    }
}
