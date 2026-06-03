package com.mailsangja.worker.service.payment;

import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.entity.payment.OrderStatus;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.OrderRepositoryPort;
import com.mailsangja.db.port.UserRepositoryPort;
import com.mailsangja.worker.common.exception.payment.PaymentErrorCode;
import com.mailsangja.worker.common.exception.payment.PaymentException;
import com.mailsangja.worker.dto.payment.PortOnePaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Test
    void process_정상웹훅이면사용자플랜을변경하고주문을완료처리한다() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String webhookId = "webhook-123";
        String paymentId = orderId.toString();
        Order order = createOrder(orderId, userId, 9900, OrderStatus.PENDING);
        User user = createUser(userId, Plan.FREE);
        PortOnePaymentResult result = new PortOnePaymentResult(paymentId, "PAID", 9900);

        when(orderRepositoryPort.findByIdWithLock(orderId)).thenReturn(Optional.of(order));
        when(userRepositoryPort.findByIdWithLock(userId)).thenReturn(Optional.of(user));

        PaymentProcessingService service = createService();

        // when
        service.process(webhookId, result);

        // then
        assertEquals(Plan.PRO, user.getPlan());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(webhookId, order.getWebhookId());
        assertEquals(paymentId, order.getPaymentId());
        verify(userRepositoryPort).save(user);
        verify(orderRepositoryPort).save(order);
    }

    @Test
    void process_이미완료된주문이면사용자조회와저장을하지않는다() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 9900, OrderStatus.COMPLETED);
        order.complete("existing-webhook", orderId.toString());
        PortOnePaymentResult result = new PortOnePaymentResult(orderId.toString(), "PAID", 9900);

        when(orderRepositoryPort.findByIdWithLock(orderId)).thenReturn(Optional.of(order));

        PaymentProcessingService service = createService();

        // when
        service.process("new-webhook", result);

        // then
        assertEquals("existing-webhook", order.getWebhookId());
        assertEquals(orderId.toString(), order.getPaymentId());
        verify(userRepositoryPort, never()).findByIdWithLock(any(UUID.class));
        verify(userRepositoryPort, never()).save(any(User.class));
        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void process_결제금액이주문금액과다르면예외를던지고저장하지않는다() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 9900, OrderStatus.PENDING);
        PortOnePaymentResult result = new PortOnePaymentResult(orderId.toString(), "PAID", 1000);

        when(orderRepositoryPort.findByIdWithLock(orderId)).thenReturn(Optional.of(order));

        PaymentProcessingService service = createService();

        // when
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> service.process("webhook-123", result)
        );

        // then
        assertSame(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH, exception.getErrorCode());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertNull(order.getWebhookId());
        verify(userRepositoryPort, never()).findByIdWithLock(any(UUID.class));
        verify(userRepositoryPort, never()).save(any(User.class));
        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void process_paymentId가UUID형식이아니면예외를던지고주문을조회하지않는다() {
        // given
        PortOnePaymentResult result = new PortOnePaymentResult("invalid-uuid", "PAID", 9900);
        PaymentProcessingService service = createService();

        // when
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> service.process("webhook-123", result)
        );

        // then
        assertSame(PaymentErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verify(orderRepositoryPort, never()).findByIdWithLock(any(UUID.class));
        verify(orderRepositoryPort, never()).save(any(Order.class));
        verify(userRepositoryPort, never()).save(any(User.class));
    }

    @Test
    void process_사용자를찾지못하면예외를던지고주문을완료처리하지않는다() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 9900, OrderStatus.PENDING);
        PortOnePaymentResult result = new PortOnePaymentResult(orderId.toString(), "PAID", 9900);

        when(orderRepositoryPort.findByIdWithLock(orderId)).thenReturn(Optional.of(order));
        when(userRepositoryPort.findByIdWithLock(userId)).thenReturn(Optional.empty());

        PaymentProcessingService service = createService();

        // when
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> service.process("webhook-123", result)
        );

        // then
        assertSame(PaymentErrorCode.PAYMENT_USER_NOT_FOUND, exception.getErrorCode());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(userRepositoryPort, never()).save(any(User.class));
        verify(orderRepositoryPort, never()).save(any(Order.class));
    }

    @Test
    void isWebhookAlreadyProcessed_저장소조회결과를그대로반환한다() {
        // given
        String webhookId = "webhook-123";
        when(orderRepositoryPort.existsByWebhookId(webhookId)).thenReturn(true);
        PaymentProcessingService service = createService();

        // when
        boolean result = service.isWebhookAlreadyProcessed(webhookId);

        // then
        assertEquals(true, result);
    }

    private PaymentProcessingService createService() {
        return new PaymentProcessingService(userRepositoryPort, orderRepositoryPort);
    }

    private Order createOrder(UUID orderId, UUID userId, int amount, OrderStatus status) {
        return Order.builder()
                .id(orderId)
                .userId(userId)
                .plan(Plan.PRO)
                .amount(amount)
                .status(status)
                .build();
    }

    private User createUser(UUID userId, Plan plan) {
        return User.builder()
                .id(userId)
                .name("tester")
                .username("tester@example.com")
                .password("encoded-password")
                .plan(plan)
                .role(Role.USER)
                .creditUsage(0)
                .build();
    }
}
