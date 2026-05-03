package com.mailsangja.core.dto.payment;

import com.mailsangja.db.entity.payment.Order;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Pre-Order 생성 응답")
public record CreateOrderResponse(

        @Schema(description = "생성된 Order PK — 포트원 결제 요청 시 merchant_uid로 사용", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID merchantUid
) {
    public static CreateOrderResponse from(Order order) {
        return new CreateOrderResponse(order.getId());
    }
}
