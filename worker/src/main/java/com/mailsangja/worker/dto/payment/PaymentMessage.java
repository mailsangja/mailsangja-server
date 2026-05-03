package com.mailsangja.worker.dto.payment;

import com.mailsangja.worker.common.exception.payment.PaymentErrorCode;
import com.mailsangja.worker.common.exception.payment.PaymentException;

public record PaymentMessage(
        String webhookId,
        String paymentId,
        String type
) {
    public PaymentMessage {
        if (webhookId == null || webhookId.isBlank()) throw new PaymentException(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, "webhookId must not be blank");
        if (paymentId == null || paymentId.isBlank()) throw new PaymentException(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, "paymentId must not be blank");
        if (type == null || type.isBlank()) throw new PaymentException(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, "type must not be blank");
    }
}
