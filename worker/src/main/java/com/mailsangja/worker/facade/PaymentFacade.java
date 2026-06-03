package com.mailsangja.worker.facade;

import com.mailsangja.worker.dto.payment.PortOnePaymentResult;
import com.mailsangja.worker.dto.payment.PortOneWebhookRequest;
import com.mailsangja.worker.service.payment.PaymentProcessingService;
import com.mailsangja.worker.service.payment.PortOneApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private static final String PAID_WEBHOOK_TYPE = "Transaction.Paid";

    private final PortOneApiService portOneApiService;
    private final PaymentProcessingService paymentProcessingService;

    public void handleWebhook(PortOneWebhookRequest request) {
        if (!PAID_WEBHOOK_TYPE.equals(request.type())) {
            log.debug("Ignored webhook type={}. webhookId={}", request.type(), request.webhookId());
            return;
        }

        if (paymentProcessingService.isWebhookAlreadyProcessed(request.webhookId())) {
            log.info("Duplicate webhook ignored. webhookId={}", request.webhookId());
            return;
        }

        PortOnePaymentResult result = portOneApiService.fetchPayment(request.data().paymentId());
        paymentProcessingService.process(request.webhookId(), result);

        log.info("Webhook handled. webhookId={} paymentId={}",
                request.webhookId(), request.data().paymentId());
    }
}
