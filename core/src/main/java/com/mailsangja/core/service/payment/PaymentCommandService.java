package com.mailsangja.core.service.payment;

import com.mailsangja.core.common.exception.payment.PaymentErrorCode;
import com.mailsangja.core.common.exception.payment.PaymentException;
import com.mailsangja.core.config.properties.PortOnePlanPriceProperties;
import com.mailsangja.core.dto.payment.CreateOrderRequest;
import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 결제 Pre-Order 생성 서비스.
 *
 * 결제 시작 전 PENDING 상태의 Order를 DB에 생성하고 반환합니다.
 * 생성된 Order의 PK가 포트원 결제 요청의 merchant_uid로 사용됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PortOnePlanPriceProperties portOnePlanPriceProperties;

    @Transactional
    public Order createPendingOrder(User user, CreateOrderRequest request) {
        String planKey = request.plan().name().toLowerCase();
        Map<String, Integer> planPrices = portOnePlanPriceProperties.getPlanPrices();
        if (planPrices == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN);
        }

        Integer amount = planPrices.get(planKey);
        if (amount == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN);
        }

        Order order = Order.builder()
                .userId(user.getId())
                .plan(request.plan())
                .amount(amount)
                .build();

        Order saved = orderRepositoryPort.save(order);
        log.info("Pre-Order created. orderId={} userId={} plan={} amount={}",
                saved.getId(), user.getId(), request.plan(), amount);
        return saved;
    }
}
