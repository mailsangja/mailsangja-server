package com.mailsangja.worker.messaging;

import com.mailsangja.worker.dto.mail.InitialMailSyncThreadBatchMessage;
import com.mailsangja.worker.dto.mail.InitialMailSyncMessage;
import com.mailsangja.worker.facade.InitialMailSyncFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialMailSyncListener {

    private final InitialMailSyncFacade initialMailSyncFacade;

    @RabbitListener(queues = "#{@initialMailSyncQueue.name}")
    public void handle(InitialMailSyncMessage message) {
        initialMailSyncFacade.handleInitialMailSync(message);
    }

    @RabbitListener(queues = "#{@initialMailSyncThreadBatchQueue.name}")
    public void handleThreadBatch(InitialMailSyncThreadBatchMessage message) {
        initialMailSyncFacade.handleInitialMailSyncThreadBatch(message);
    }
}
