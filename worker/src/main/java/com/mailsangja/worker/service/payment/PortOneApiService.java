package com.mailsangja.worker.service.payment;

import com.mailsangja.worker.common.exception.payment.PaymentErrorCode;
import com.mailsangja.worker.common.exception.payment.PaymentException;
import com.mailsangja.worker.config.properties.PortOneProperties;
import com.mailsangja.worker.dto.payment.PortOnePaymentResponse;
import com.mailsangja.worker.dto.payment.PortOnePaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class PortOneApiService {

    private final PortOneProperties portOneProperties;
    private final RestClient portOneRestClient;

    public PortOneApiService(
            PortOneProperties portOneProperties,
            @Qualifier("portOneRestClient") RestClient portOneRestClient
    ) {
        this.portOneProperties = portOneProperties;
        this.portOneRestClient = portOneRestClient;
    }

    public PortOnePaymentResult fetchPayment(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_WEBHOOK_INVALID, "paymentId must not be blank");
        }

        String uri = UriComponentsBuilder.fromUriString(portOneProperties.getPaymentQueryUri())
                .pathSegment(paymentId)
                .toUriString();

        try {
            PortOnePaymentResponse response = portOneRestClient
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "PortOne " + portOneProperties.getApiSecret())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PortOnePaymentResponse.class);

            if (response == null || response.id() == null) {
                log.warn("PortOne payment response is null or missing id. paymentId={}", paymentId);
                throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND);
            }

            validatePaymentStatus(response);
            return response.toResult();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.error("PortOne payment client error. paymentId={} status={}", paymentId, e.getStatusCode());
                throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND);
            }
            log.error("PortOne payment server error. paymentId={} status={}", paymentId, e.getStatusCode());
            throw new PaymentException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (RestClientException e) {
            log.error("PortOne payment fetch failed. paymentId={} error={}", paymentId, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    private void validatePaymentStatus(PortOnePaymentResponse response) {
        if (!"PAID".equals(response.status())) {
            log.warn("Payment status is not PAID. paymentId={} status={}", response.id(), response.status());
            throw new PaymentException(PaymentErrorCode.PAYMENT_STATUS_INVALID);
        }
    }
}
