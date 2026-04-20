package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.fcm.RegisterFcmTokenRequest;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "FCM Token", description = "Firebase 푸시 알림 토큰 API")
public interface FcmTokenControllerDocs {

    @Operation(
            summary = "FCM 디바이스 토큰 등록",
            description = "현재 로그인된 사용자의 디바이스 FCM 토큰을 등록합니다. 이미 등록된 토큰이면 멱등 처리됩니다. SESSION 쿠키가 필요합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 등록 성공",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 FCM 토큰",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 (로그인되지 않은 상태)",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<Void> registerFcmToken(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody(
                    description = "FCM 디바이스 토큰 등록 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegisterFcmTokenRequest.class))
            )
            RegisterFcmTokenRequest request
    );
}
