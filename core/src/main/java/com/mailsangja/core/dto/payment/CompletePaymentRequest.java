package com.mailsangja.core.dto.payment;

import com.mailsangja.core.common.exception.payment.PaymentErrorCode;
import com.mailsangja.core.common.exception.payment.PaymentException;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 완료 요청")
public record CompletePaymentRequest(

        @Schema(description = "포트원 결제 ID", example = "payment-uuid-1234")
        String paymentId
) {
    public CompletePaymentRequest {
        if (paymentId == null || paymentId.isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ID_BLANK);
        }
    }
}
