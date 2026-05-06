package com.mailsangja.core.dto.payment;

import com.mailsangja.core.common.exception.payment.PaymentErrorCode;
import com.mailsangja.core.common.exception.payment.PaymentException;
import com.mailsangja.db.entity.user.Plan;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pre-Order 생성 요청")
public record CreateOrderRequest(

        @Schema(description = "업그레이드 대상 플랜", example = "PRO", allowableValues = {"FREE", "PRO"})
        Plan plan
) {
    public CreateOrderRequest {
        if (plan == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN);
        }
    }
}
