package com.mailsangja.core.dto.payment;

public record PortOnePaymentResult(
        String paymentId,
        String status,
        int amount
) {
}
