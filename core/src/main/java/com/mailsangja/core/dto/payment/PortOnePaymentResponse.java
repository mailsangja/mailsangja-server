package com.mailsangja.core.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
        String id,
        String merchantUid,
        String status,
        Amount amount,
        CustomData customData
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(int total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomData(String planCode) {
    }

    public PortOnePaymentResult toResult() {
        String planCode = customData != null ? customData.planCode() : null;
        int totalAmount = amount != null ? amount.total() : 0;
        return new PortOnePaymentResult(id, merchantUid, status, totalAmount, planCode);
    }
}
