package com.mailsangja.worker.controller.docs;

import com.mailsangja.worker.dto.payment.PortOneWebhookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Payment", description = "결제 웹훅 수신 API")
public interface PaymentControllerDocs {

    @Operation(
            summary = "포트원 결제 완료 웹훅",
            description = """
                    포트원 V2 결제 완료 웹훅을 수신하여 사용자 플랜을 업그레이드합니다.

                    포트원이 결제 완료 후 이 엔드포인트를 POST로 호출합니다.
                    웹훅 타입이 'Transaction.Paid'일 때만 처리되며, 결제 금액과 상태를 검증한 후 플랜을 변경합니다.
                    webhookId(루트 레벨)를 멱등성 키로 사용하며, 동일한 webhookId가 재수신되면 즉시 200으로 반환합니다.

                    클라이언트는 결제 요청 시 merchant_uid에 Pre-Order 생성 API에서 받은 merchantUid를 담아야 합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "웹훅 처리 성공",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 웹훅 요청",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<Void> handlePaymentWebhook(
            @RequestBody(
                    description = "포트원 결제 완료 웹훅 페이로드",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PortOneWebhookRequest.class))
            )
            PortOneWebhookRequest request
    );
}
