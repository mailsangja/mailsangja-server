package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.port.OrderRepositoryPort;
import com.mailsangja.worker.common.exception.payment.PaymentErrorCode;
import com.mailsangja.worker.common.exception.payment.PaymentException;
import com.mailsangja.worker.dto.payment.PaymentMessage;
import com.mailsangja.worker.dto.payment.PortOnePaymentResult;
import com.mailsangja.worker.service.payment.PaymentProcessingService;
import com.mailsangja.worker.service.payment.PortOneApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 포트원 결제 웹훅 이벤트 Consumer.
 *
 * 처리 순서:
 *   webhookId 중복 확인 (1차 멱등성 — Application Level)
 *   포트원 단건 조회 API 호출 + 결제 상태(PAID) 검증 (트랜잭션 밖)
 *   {@link PaymentProcessingService#process} 호출 — DB 원본 금액 교차 검증 + PENDING Order COMPLETED 전환 + User 플랜 업그레이드
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final String PAID_WEBHOOK_TYPE = "Transaction.Paid";

    private final OrderRepositoryPort orderRepositoryPort;
    private final PortOneApiService portOneApiService;
    private final PaymentProcessingService paymentProcessingService;

    @RabbitListener(
            queues = "#{@paymentWebhookQueue.name}",
            containerFactory = "paymentWebhookRabbitListenerContainerFactory"
    )
    public void handle(PaymentMessage message) {
        String webhookId = message.webhookId();
        String paymentId = message.paymentId();

        if (!PAID_WEBHOOK_TYPE.equals(message.type())) {
            log.debug("Ignored webhook type={}. webhookId={}", message.type(), webhookId);
            return;
        }

        if (orderRepositoryPort.existsByWebhookId(webhookId)) {
            log.info("Duplicate webhook ignored. webhookId={}", webhookId);
            return;
        }

        // 외부 API 호출 + 결제 상태 검증 — 트랜잭션 밖
        PortOnePaymentResult result = portOneApiService.fetchPayment(paymentId);

        Plan plan = resolvePlan(result);
        paymentProcessingService.process(webhookId, result, plan);

        log.info("Payment webhook handled. webhookId={} paymentId={} merchantUid={} plan={}",
                webhookId, paymentId, result.merchantUid(), plan);
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
            throw new PaymentException(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN,
                    "Unknown planCode: " + planCode);
        }
    }
}
