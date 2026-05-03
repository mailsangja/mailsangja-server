package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.PaymentControllerDocs;
import com.mailsangja.core.dto.payment.CreateOrderRequest;
import com.mailsangja.core.dto.payment.CreateOrderResponse;
import com.mailsangja.core.facade.PaymentFacade;
import com.mailsangja.db.entity.user.User;
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
    @PostMapping("/api/v1/payments")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @AuthUser User user,
            @RequestBody CreateOrderRequest request
    ) {
        CreateOrderResponse response = paymentFacade.createOrder(user, request);
        return ResponseEntity.ok(response);
    }
}
