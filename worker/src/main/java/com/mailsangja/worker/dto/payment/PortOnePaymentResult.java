package com.mailsangja.worker.dto.payment;

public record PortOnePaymentResult(
        String paymentId,
        String status,
        int amount
) {
}
