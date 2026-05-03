package com.mailsangja.worker.controller;

import com.mailsangja.worker.controller.docs.PaymentControllerDocs;
import com.mailsangja.worker.dto.payment.PortOneWebhookRequest;
import com.mailsangja.worker.facade.PaymentFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentControllerDocs {

    private final PaymentFacade paymentFacade;

    @Override
    @PostMapping("/api/v1/payments/webhook")
    public ResponseEntity<Void> handlePaymentWebhook(@RequestBody PortOneWebhookRequest request) {
        paymentFacade.publishWebhook(request);
        return ResponseEntity.ok().build();
    }
}
