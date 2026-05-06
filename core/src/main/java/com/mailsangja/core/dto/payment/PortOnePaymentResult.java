package com.mailsangja.core.dto.payment;

public record PortOnePaymentResult(
        String paymentId,
        String merchantUid,
        String status,
        int amount,
        String planCode
) {
}
