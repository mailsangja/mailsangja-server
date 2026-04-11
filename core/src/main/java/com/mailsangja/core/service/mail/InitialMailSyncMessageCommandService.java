package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.core.config.properties.MailTaskRabbitProperties;
import com.mailsangja.core.dto.mail.InitialMailSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitialMailSyncMessageCommandService {

    private final RabbitTemplate rabbitTemplate;
    private final InitialMailSyncRabbitProperties initialMailSyncRabbitProperties;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;

    public void publish(InitialMailSyncMessage message) {
        validateMessage(message);

        try {
            rabbitTemplate.convertAndSend(
                    mailTaskRabbitProperties.getExchange(),
                    initialMailSyncRabbitProperties.getRoutingKey(),
                    message,
                    new CorrelationData(message.mailAccountId().toString())
            );

            log.info(
                    "Published initial mail sync request for mailAccountId={} userId={} provider={} emailAddress={}",
                    message.mailAccountId(),
                    message.userId(),
                    message.provider(),
                    message.emailAddress()
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish initial mail sync request for mailAccountId={} userId={} provider={} emailAddress={}",
                    message.mailAccountId(),
                    message.userId(),
                    message.provider(),
                    message.emailAddress(),
                    e
            );
        }
    }

    private void validateMessage(InitialMailSyncMessage message) {
        if (message == null) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_INITIAL_MAIL_SYNC_MESSAGE);
        }

        if (message.mailAccountId() == null
                || message.userId() == null
                || !message.isGoogleMailAccount()
                || isBlank(message.emailAddress())
                || isBlank(initialMailSyncRabbitProperties.getTaskName())
                || isBlank(mailTaskRabbitProperties.getExchange())
                || isBlank(initialMailSyncRabbitProperties.getRoutingKey())) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_INITIAL_MAIL_SYNC_MESSAGE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
