package com.mailsangja.core.service.label;

import com.mailsangja.core.config.properties.LabelReclassifyRabbitProperties;
import com.mailsangja.core.config.properties.MailTaskRabbitProperties;
import com.mailsangja.core.dto.label.LabelReclassifyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelReclassifyPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final LabelReclassifyRabbitProperties labelReclassifyRabbitProperties;

    public void publish(LabelReclassifyMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    mailTaskRabbitProperties.getExchange(),
                    labelReclassifyRabbitProperties.getRoutingKey(),
                    message,
                    new CorrelationData(message.userId() + ":" + message.labelIds().size())
            );
            log.info(
                    "Published label reclassify request for userId={} labelCount={}",
                    message.userId(),
                    message.labelIds().size()
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish label reclassify request for userId={} labelCount={}",
                    message.userId(),
                    message.labelIds().size(),
                    e
            );
            throw e;
        }
    }
}
