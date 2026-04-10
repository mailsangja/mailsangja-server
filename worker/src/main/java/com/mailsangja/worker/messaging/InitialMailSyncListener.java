package com.mailsangja.worker.messaging;

import com.mailsangja.worker.dto.mail.InitialMailSyncMessage;
import com.mailsangja.worker.facade.InitialMailSyncFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialMailSyncListener {

    private final InitialMailSyncFacade initialMailSyncFacade;

    @RabbitListener(queues = "${mailsangja.rabbitmq.initial-mail-sync.queue}")
    public void handle(InitialMailSyncMessage message) {
        initialMailSyncFacade.handleInitialMailSync(message);
    }
}
