package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.core.dto.mail.InitialMailSyncCommand;
import com.mailsangja.db.entity.mail.MailAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitialMailSyncMessageCommandService {

    private final RabbitTemplate rabbitTemplate;
    private final InitialMailSyncRabbitProperties initialMailSyncRabbitProperties;

    public void publish(MailAccount mailAccount) {
        InitialMailSyncCommand command = InitialMailSyncCommand.from(mailAccount);
        validateCommand(command);

        try {
            rabbitTemplate.convertAndSend(
                    initialMailSyncRabbitProperties.getExchange(),
                    initialMailSyncRabbitProperties.getRoutingKey(),
                    command
            );

            log.info(
                    "Published initial mail sync request for mailAccountId={} userId={} provider={} emailAddress={}",
                    command.mailAccountId(),
                    command.userId(),
                    command.provider(),
                    command.emailAddress()
            );
        } catch (AmqpException e) {
            log.warn(
                    "Failed to publish initial mail sync request for mailAccountId={} userId={} provider={} emailAddress={}",
                    command.mailAccountId(),
                    command.userId(),
                    command.provider(),
                    command.emailAddress(),
                    e
            );
        }
    }

    private void validateCommand(InitialMailSyncCommand command) {
        if (command.mailAccountId() == null
                || command.userId() == null
                || !command.isGoogleMailAccount()
                || isBlank(command.emailAddress())
                || isBlank(initialMailSyncRabbitProperties.getExchange())
                || isBlank(initialMailSyncRabbitProperties.getRoutingKey())) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
