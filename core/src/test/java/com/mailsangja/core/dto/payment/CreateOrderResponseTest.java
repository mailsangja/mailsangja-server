package com.mailsangja.core.dto.payment;

import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.payment.OrderStatus;
import com.mailsangja.db.entity.user.Plan;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreateOrderResponseTest {

    @Test
    void from_주문정보와금액상태를응답으로매핑한다() {
        // given
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .plan(Plan.PRO)
                .amount(9900)
                .status(OrderStatus.PENDING)
                .build();

        // when
        CreateOrderResponse response = CreateOrderResponse.from(order);

        // then
        assertEquals(orderId, response.paymentId());
        assertEquals(Plan.PRO, response.plan());
        assertEquals(9900, response.amount());
        assertEquals(OrderStatus.PENDING, response.status());
        assertNull(response.createdAt());
    }
}
