package com.mailsangja.worker.service.payment;

import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.payment.OrderStatus;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.OrderRepositoryPort;
import com.mailsangja.db.port.UserRepositoryPort;
import com.mailsangja.worker.common.exception.payment.PaymentErrorCode;
import com.mailsangja.worker.common.exception.payment.PaymentException;
import com.mailsangja.worker.dto.payment.PortOnePaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final UserRepositoryPort userRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;

    public boolean isWebhookAlreadyProcessed(String webhookId) {
        return orderRepositoryPort.existsByWebhookId(webhookId);
    }

    @Transactional
    public void process(String webhookId, PortOnePaymentResult result) {
        UUID orderId = parseOrderId(result.paymentId());

        Order order = orderRepositoryPort.findByIdWithLock(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "orderId=" + orderId));

        if (OrderStatus.COMPLETED.equals(order.getStatus())) {
            log.info("Order already completed, skipping. orderId={} webhookId={}", orderId, webhookId);
            return;
        }

        validatePaymentAmount(result, order);

        UUID userId = order.getUserId();

        User user = userRepositoryPort.findByIdWithLock(userId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_USER_NOT_FOUND, "userId=" + userId));

        user.updatePlan(order.getPlan());
        userRepositoryPort.save(user);

        order.complete(webhookId, result.paymentId());
        orderRepositoryPort.save(order);

        log.info("Payment processing completed. webhookId={} orderId={} userId={} plan={}",
                webhookId, orderId, userId, order.getPlan());
    }

    private void validatePaymentAmount(PortOnePaymentResult result, Order order) {
        if (result.amount() != order.getAmount()) {
            log.warn("Amount mismatch. paymentId={} expected={} actual={}",
                    result.paymentId(), order.getAmount(), result.amount());
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private UUID parseOrderId(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "paymentId is blank");
        }
        try {
            return UUID.fromString(paymentId);
        } catch (IllegalArgumentException e) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND,
                    "paymentId is not a valid UUID: " + paymentId);
        }
    }
}
