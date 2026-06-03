package com.mailsangja.worker.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
        String id,
        String status,
        Amount amount
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(int total) {
    }

    public PortOnePaymentResult toResult() {
        int totalAmount = amount != null ? amount.total() : 0;
        return new PortOnePaymentResult(id, status, totalAmount);
    }
}
