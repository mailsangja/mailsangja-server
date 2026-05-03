package com.mailsangja.worker.dto.payment;

import com.mailsangja.worker.common.exception.payment.PaymentErrorCode;
import com.mailsangja.worker.common.exception.payment.PaymentException;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포트원 결제 웹훅 요청")
public record PortOneWebhookRequest(

        @Schema(description = "포트원 웹훅 고유 식별자 (멱등성 키)", example = "webhook-uuid-5678")
        String webhookId,

        @Schema(description = "웹훅 이벤트 타입 (예: Transaction.Paid)", example = "Transaction.Paid")
        String type,

        @Schema(description = "웹훅 이벤트 데이터")
        WebhookData data
) {
    public PortOneWebhookRequest {
        if (webhookId == null || webhookId.isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, "webhookId must not be blank");
        }
        if (type == null || type.isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, "type must not be blank");
        }
        if (data == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, "data must not be null");
        }
    }

    public PaymentMessage toMessage() {
        return new PaymentMessage(webhookId, data.paymentId(), type);
    }

    @Schema(description = "웹훅 이벤트 데이터 상세")
    public record WebhookData(

            @Schema(description = "포트원 결제 ID", example = "payment-uuid-1234")
            String paymentId
    ) {
        public WebhookData {
            if (paymentId == null || paymentId.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, "paymentId must not be blank");
            }
        }
    }
}
