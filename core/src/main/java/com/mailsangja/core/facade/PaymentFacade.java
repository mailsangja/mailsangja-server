package com.mailsangja.core.facade;

import com.mailsangja.core.dto.payment.CreateOrderRequest;
import com.mailsangja.core.dto.payment.CreateOrderResponse;
import com.mailsangja.core.service.payment.PaymentCommandService;
import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentCommandService paymentCommandService;

    public CreateOrderResponse createOrder(User user, CreateOrderRequest request) {
        Order order = paymentCommandService.createPendingOrder(user, request);
        return CreateOrderResponse.from(order);
    }
}
