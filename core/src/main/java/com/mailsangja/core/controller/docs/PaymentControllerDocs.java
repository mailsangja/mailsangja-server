package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.payment.CompletePaymentRequest;
import com.mailsangja.core.dto.payment.CreateOrderRequest;
import com.mailsangja.core.dto.payment.CreateOrderResponse;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Payment", description = "결제 및 플랜 업그레이드 API")
public interface PaymentControllerDocs {

    @Operation(
            summary = "Pre-Order 생성",
            description = """
                    결제 시작 전 서버에 PENDING 상태의 Pre-Order를 생성합니다.

                    클라이언트는 plan(PRO)을 전달합니다.
                    서버는 PENDING Order를 생성하고 PK(UUID), 플랜, 금액, 주문 상태를 반환합니다.
                    클라이언트는 응답의 paymentId를 포트원 결제 SDK의 paymentId 필드에 그대로 사용합니다.
"""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Pre-Order 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateOrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 요청 (plan 누락 또는 알 수 없는 플랜)",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<CreateOrderResponse> createOrder(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody(
                    description = "Pre-Order 생성 요청 (plan)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateOrderRequest.class))
            )
            CreateOrderRequest request
    );

    @Operation(
            summary = "결제 완료 처리",
            description = """
                    포트원 결제 완료 후 클라이언트가 호출하는 결제 확정 API입니다.

                    클라이언트는 포트원으로부터 받은 paymentId를 전달합니다.
                    서버는 포트원 API로 결제 정보를 검증한 뒤 주문 상태를 COMPLETED로 전환하고 사용자 플랜을 업그레이드합니다.
                    웹훅이 먼저 처리된 경우 멱등성 처리되어 정상 응답합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 완료 처리 성공",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "결제 상태 또는 금액 불일치",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "주문 또는 결제 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<Void> completePayment(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody(
                    description = "결제 완료 요청 (paymentId)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CompletePaymentRequest.class))
            )
            CompletePaymentRequest request
    );
}
