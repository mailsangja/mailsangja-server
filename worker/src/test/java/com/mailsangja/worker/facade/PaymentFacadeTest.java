package com.mailsangja.worker.facade;

import com.mailsangja.worker.common.exception.payment.PaymentErrorCode;
import com.mailsangja.worker.common.exception.payment.PaymentException;
import com.mailsangja.worker.dto.payment.PortOnePaymentResult;
import com.mailsangja.worker.dto.payment.PortOneWebhookRequest;
import com.mailsangja.worker.service.payment.PaymentProcessingService;
import com.mailsangja.worker.service.payment.PortOneApiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @Mock
    private PortOneApiService portOneApiService;

    @Mock
    private PaymentProcessingService paymentProcessingService;

    @Test
    void handleWebhook_결제완료이외타입이면포트원조회와처리를하지않는다() {
        // given
        PortOneWebhookRequest request = createRequest("webhook-123", "Transaction.Cancelled", "payment-123");
        PaymentFacade facade = createFacade();

        // when
        facade.handleWebhook(request);

        // then
        verify(paymentProcessingService, never()).isWebhookAlreadyProcessed(request.webhookId());
        verify(portOneApiService, never()).fetchPayment(request.data().paymentId());
        verify(paymentProcessingService, never()).process(eq(request.webhookId()), any(PortOnePaymentResult.class));
    }

    @Test
    void handleWebhook_이미처리된webhookId면포트원조회와처리를하지않는다() {
        // given
        PortOneWebhookRequest request = createRequest("webhook-123", "Transaction.Paid", "payment-123");
        when(paymentProcessingService.isWebhookAlreadyProcessed(request.webhookId())).thenReturn(true);
        PaymentFacade facade = createFacade();

        // when
        facade.handleWebhook(request);

        // then
        verify(paymentProcessingService).isWebhookAlreadyProcessed(request.webhookId());
        verify(portOneApiService, never()).fetchPayment(request.data().paymentId());
        verify(paymentProcessingService, never()).process(eq(request.webhookId()), any(PortOnePaymentResult.class));
    }

    @Test
    void handleWebhook_정상결제웹훅이면결제정보조회후처리한다() {
        // given
        UUID orderId = UUID.randomUUID();
        PortOneWebhookRequest request = createRequest("webhook-123", "Transaction.Paid", "payment-123");
        PortOnePaymentResult result = new PortOnePaymentResult(orderId.toString(), "PAID", 9900);

        when(paymentProcessingService.isWebhookAlreadyProcessed(request.webhookId())).thenReturn(false);
        when(portOneApiService.fetchPayment(request.data().paymentId())).thenReturn(result);

        PaymentFacade facade = createFacade();

        // when
        facade.handleWebhook(request);

        // then
        verify(portOneApiService).fetchPayment(request.data().paymentId());
        verify(paymentProcessingService).process(request.webhookId(), result);
    }

    @Test
    void portOneWebhookRequest_webhookId가비어있으면예외를던진다() {
        // given
        String webhookId = " ";

        // when
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> createRequest(webhookId, "Transaction.Paid", "payment-123")
        );

        // then
        assertSame(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, exception.getErrorCode());
    }

    @Test
    void portOneWebhookRequest_paymentId가비어있으면예외를던진다() {
        // given
        String paymentId = " ";

        // when
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> createRequest("webhook-123", "Transaction.Paid", paymentId)
        );

        // then
        assertSame(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, exception.getErrorCode());
    }

    private PaymentFacade createFacade() {
        return new PaymentFacade(portOneApiService, paymentProcessingService);
    }

    private PortOneWebhookRequest createRequest(String webhookId, String type, String paymentId) {
        return new PortOneWebhookRequest(
                webhookId,
                type,
                new PortOneWebhookRequest.WebhookData(paymentId)
        );
    }
}
