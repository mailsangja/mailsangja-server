package com.mailsangja.worker.facade;

import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.worker.common.exception.payment.PaymentErrorCode;
import com.mailsangja.worker.common.exception.payment.PaymentException;
import com.mailsangja.worker.dto.payment.PortOnePaymentResult;
import com.mailsangja.worker.dto.payment.PortOneWebhookRequest;
import com.mailsangja.worker.service.payment.PaymentProcessingService;
import com.mailsangja.worker.service.payment.PortOneApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

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
        Plan plan = resolvePlan(result);
        paymentProcessingService.process(request.webhookId(), result, plan);

        log.info("Webhook handled. webhookId={} paymentId={} plan={}",
                request.webhookId(), request.data().paymentId(), plan);
    }

    private Plan resolvePlan(PortOnePaymentResult result) {
        String planCode = result.planCode();
        if (planCode == null || planCode.isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN,
                    "planCode is missing in customData. paymentId=" + result.paymentId());
        }
        try {
            return Plan.valueOf(planCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN, "Unknown planCode: " + planCode);
        }
    }
}
