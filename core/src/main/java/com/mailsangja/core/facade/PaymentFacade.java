package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.payment.PaymentErrorCode;
import com.mailsangja.core.common.exception.payment.PaymentException;
import com.mailsangja.core.dto.payment.CompletePaymentRequest;
import com.mailsangja.core.dto.payment.CreateOrderRequest;
import com.mailsangja.core.dto.payment.CreateOrderResponse;
import com.mailsangja.core.dto.payment.PortOnePaymentResult;
import com.mailsangja.core.service.payment.PaymentCommandService;
import com.mailsangja.core.service.payment.PaymentProcessingService;
import com.mailsangja.core.service.payment.PortOneApiService;
import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentCommandService paymentCommandService;
    private final PortOneApiService portOneApiService;
    private final PaymentProcessingService paymentProcessingService;

    public CreateOrderResponse createOrder(User user, CreateOrderRequest request) {
        Order order = paymentCommandService.createPendingOrder(user, request);
        return CreateOrderResponse.from(order);
    }

    public void completePayment(User user, CompletePaymentRequest request) {
        PortOnePaymentResult result = portOneApiService.fetchPayment(request.paymentId());
        Plan plan = resolvePlan(result);
        paymentProcessingService.process(null, result, plan);

        log.info("Payment completed by client. userId={} paymentId={} plan={}",
                user.getId(), request.paymentId(), plan);
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
