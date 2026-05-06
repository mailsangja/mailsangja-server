package com.mailsangja.worker.dto.payment;

public record PortOnePaymentResult(
        String paymentId,
        String merchantUid,
        String status,
        int amount,
        String planCode
) {
}
