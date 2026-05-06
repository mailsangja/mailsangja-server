package com.mailsangja.core.dto.payment;

import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.payment.OrderStatus;
import com.mailsangja.db.entity.user.Plan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Pre-Order 생성 응답")
public record CreateOrderResponse(

        @Schema(description = "생성된 Order PK — 포트원 결제 요청 시 merchant_uid로 사용", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID merchantUid,

        @Schema(description = "주문 플랜", example = "PRO")
        Plan plan,

        @Schema(description = "결제 금액", example = "9900")
        int amount,

        @Schema(description = "주문 상태", example = "PENDING")
        OrderStatus status,

        @Schema(description = "주문 생성 시각", example = "2026-05-06T12:34:56")
        LocalDateTime createdAt
) {
    public static CreateOrderResponse from(Order order) {
        return new CreateOrderResponse(
                order.getId(),
                order.getPlan(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
