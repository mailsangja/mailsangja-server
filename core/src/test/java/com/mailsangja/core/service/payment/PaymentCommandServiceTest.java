package com.mailsangja.core.service.payment;

import com.mailsangja.core.common.exception.payment.PaymentErrorCode;
import com.mailsangja.core.common.exception.payment.PaymentException;
import com.mailsangja.core.config.properties.PortOnePlanPriceProperties;
import com.mailsangja.core.dto.payment.CreateOrderRequest;
import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.payment.OrderStatus;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.OrderRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Test
    void createPendingOrder_PRO플랜이면가격설정금액으로PENDING주문을저장한다() {
        // given
        UUID savedOrderId = UUID.randomUUID();
        User user = createUser();
        PortOnePlanPriceProperties properties = createProperties(Map.of("pro", 9900));
        PaymentCommandService service = new PaymentCommandService(orderRepositoryPort, properties);

        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return Order.builder()
                    .id(savedOrderId)
                    .userId(order.getUserId())
                    .plan(order.getPlan())
                    .amount(order.getAmount())
                    .status(order.getStatus())
                    .build();
        });

        // when
        Order result = service.createPendingOrder(user, new CreateOrderRequest(Plan.PRO));

        // then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepositoryPort).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertEquals(user.getId(), savedOrder.getUserId());
        assertEquals(Plan.PRO, savedOrder.getPlan());
        assertEquals(9900, savedOrder.getAmount());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(savedOrderId, result.getId());
        assertEquals(9900, result.getAmount());
        assertEquals(OrderStatus.PENDING, result.getStatus());
    }

    @Test
    void createPendingOrder_요청플랜의가격설정이없으면예외를던지고저장하지않는다() {
        // given
        User user = createUser();
        PortOnePlanPriceProperties properties = createProperties(Map.of("free", 0));
        PaymentCommandService service = new PaymentCommandService(orderRepositoryPort, properties);

        // when
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> service.createPendingOrder(user, new CreateOrderRequest(Plan.PRO))
        );

        // then
        assertSame(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN, exception.getErrorCode());
        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void createPendingOrder_가격설정맵이null이면예외를던지고저장하지않는다() {
        // given
        User user = createUser();
        PortOnePlanPriceProperties properties = createProperties(null);
        PaymentCommandService service = new PaymentCommandService(orderRepositoryPort, properties);

        // when
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> service.createPendingOrder(user, new CreateOrderRequest(Plan.PRO))
        );

        // then
        assertSame(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN, exception.getErrorCode());
        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void createOrderRequest_플랜이null이면예외를던진다() {
        // given
        Plan plan = null;

        // when
        PaymentException exception = assertThrows(PaymentException.class, () -> new CreateOrderRequest(plan));

        // then
        assertSame(PaymentErrorCode.PAYMENT_PLAN_UNKNOWN, exception.getErrorCode());
    }

    private PortOnePlanPriceProperties createProperties(Map<String, Integer> planPrices) {
        PortOnePlanPriceProperties properties = new PortOnePlanPriceProperties();
        properties.setPlanPrices(planPrices);
        return properties;
    }

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("tester")
                .username("tester@example.com")
                .password("encoded-password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .creditUsage(0)
                .build();
    }
}
