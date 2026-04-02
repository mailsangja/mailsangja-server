package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;

@Tag(name = "Mail Account", description = "메일 계정 연동 API")
public interface MailAccountControllerDocs {

    @Operation(
            summary = "Google OAuth 인가 URL 생성",
            description = "로그인한 사용자의 세션에 OAuth state와 userId를 저장하고 Google OAuth 인가 URL을 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인가 URL 생성 성공",
                    content = @Content(schema = @Schema(implementation = MailAccountAuthorizeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<MailAccountAuthorizeResponse> authorizeGoogle(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(hidden = true) HttpSession session
    );

    @Operation(
            summary = "Google OAuth 콜백 처리",
            description = "Google에서 전달한 code와 state를 검증한 뒤 토큰 교환, 사용자 정보 조회, MailAccount 저장을 수행합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "메일 계정 연동 성공",
                    content = @Content(schema = @Schema(implementation = MailAccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "인가 코드, state, OAuth 응답값 또는 refresh token 이 유효하지 않음",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "OAuth 세션 정보 없음 또는 인증 필요",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "OAuth 요청 사용자 정보 불일치",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 연결된 메일 계정",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Google OAuth 토큰 교환 또는 사용자 정보 조회 실패",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<MailAccountResponse> googleCallback(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "Google OAuth 인가 코드", required = true, example = "4/0AQSTgQ...")
            String code,
            @Parameter(description = "세션에 저장한 OAuth state", required = true, example = "c4c6f8c2-3b2b-4c5b-9f2c-7a1d3f9a9f11")
            String state,
            @Parameter(hidden = true) HttpSession session
    );
}
