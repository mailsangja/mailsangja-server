package com.mailsangja.core.dto.payment;

import com.mailsangja.db.entity.user.Plan;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pre-Order 생성 요청")
public record CreateOrderRequest(

        @Schema(description = "업그레이드 대상 플랜", example = "PRO", allowableValues = {"FREE", "PRO"})
        Plan plan,

        @Schema(description = "결제 예정 금액 (원)", example = "9900")
        int amount
) {
    public CreateOrderRequest {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
