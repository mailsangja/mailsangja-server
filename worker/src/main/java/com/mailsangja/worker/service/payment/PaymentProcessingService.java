package com.mailsangja.worker.service.payment;

import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.payment.OrderStatus;
import com.mailsangja.db.entity.user.Plan;
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
    public void process(String webhookId, PortOnePaymentResult result, Plan plan) {
        UUID merchantUid = parseMerchantUid(result.merchantUid());

        Order order = orderRepositoryPort.findByIdWithLock(merchantUid)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "merchantUid=" + merchantUid));

        if (OrderStatus.COMPLETED.equals(order.getStatus())) {
            log.info("Order already completed, skipping. merchantUid={} webhookId={}", merchantUid, webhookId);
            return;
        }

        validatePaymentAmount(result, order);

        UUID userId = order.getUserId();

        User user = userRepositoryPort.findByIdWithLock(userId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_USER_NOT_FOUND, "userId=" + userId));

        user.updatePlan(plan);
        userRepositoryPort.save(user);

        order.complete(webhookId, result.paymentId());
        orderRepositoryPort.save(order);

        log.info("Payment processing completed. webhookId={} merchantUid={} userId={} plan={}",
                webhookId, merchantUid, userId, plan);
    }

    private void validatePaymentAmount(PortOnePaymentResult result, Order order) {
        if (result.amount() != order.getAmount()) {
            log.warn("Amount mismatch. paymentId={} merchantUid={} expected={} actual={}",
                    result.paymentId(), result.merchantUid(), order.getAmount(), result.amount());
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private UUID parseMerchantUid(String merchantUid) {
        if (merchantUid == null || merchantUid.isBlank()) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "merchantUid is blank");
        }
        try {
            return UUID.fromString(merchantUid);
        } catch (IllegalArgumentException e) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND,
                    "merchantUid is not a valid UUID: " + merchantUid);
        }
    }
}
