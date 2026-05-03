package com.mailsangja.worker.facade;

import com.mailsangja.worker.dto.payment.PortOneWebhookRequest;
import com.mailsangja.worker.messaging.publisher.PaymentWebhookPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentWebhookPublisher paymentWebhookPublisher;

    public void publishWebhook(PortOneWebhookRequest request) {
        paymentWebhookPublisher.publish(request);
    }
}
