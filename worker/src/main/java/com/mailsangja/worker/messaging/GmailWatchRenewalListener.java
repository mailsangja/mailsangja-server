package com.mailsangja.worker.messaging;

import com.mailsangja.worker.dto.mail.watch.WatchRenewalMessage;
import com.mailsangja.worker.facade.MailAccountFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GmailWatchRenewalListener {

    private final MailAccountFacade mailAccountFacade;

    @RabbitListener(
            queues = "#{@watchRenewalQueue.name}",
            containerFactory = "watchRenewalRabbitListenerContainerFactory"
    )
    public void handle(WatchRenewalMessage message) {
        mailAccountFacade.handleWatchRenewal(message);
    }
}
