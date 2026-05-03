package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.payment.CreateOrderRequest;
import com.mailsangja.core.dto.payment.CreateOrderResponse;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
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

                    클라이언트는 plan(PRO)과 amount(결제 예정 금액)를 전달합니다.
                    서버는 PENDING Order를 생성하고 PK(UUID)를 merchantUid로 반환합니다.
                    클라이언트는 이 merchantUid를 포트원 결제 요청의 merchant_uid 필드에 그대로 사용합니다.
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
            @AuthUser User user,
            @RequestBody(
                    description = "Pre-Order 생성 요청 (plan, amount)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateOrderRequest.class))
            )
            CreateOrderRequest request
    );
}
