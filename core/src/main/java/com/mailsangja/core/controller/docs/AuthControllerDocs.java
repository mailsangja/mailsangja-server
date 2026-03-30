package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.auth.LoginRequest;
import com.mailsangja.core.dto.auth.RegisterRequest;
import com.mailsangja.core.dto.auth.UserInfoResponse;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "인증 API (회원가입, 로그인, 내 정보 조회)")
public interface AuthControllerDocs {

    @Operation(
            summary = "회원가입",
            description = "새로운 사용자 계정을 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 사용 중인 아이디",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<UserInfoResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "회원가입 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequest.class))
            )
            RegisterRequest request
    );

    @Operation(
            summary = "로그인",
            description = "아이디와 비밀번호로 인증하며, 성공 시 세션 쿠키(SESSION)가 발급됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 (응답 헤더의 Set-Cookie로 SESSION 쿠키가 설정됩니다)",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "아이디 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<UserInfoResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로그인 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            LoginRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest,
            @Parameter(hidden = true) HttpServletResponse httpResponse
    );

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인된 사용자의 정보를 반환합니다. SESSION 쿠키가 필요합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 (로그인되지 않은 상태)",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<UserInfoResponse> me(@Parameter(hidden = true) @AuthUser User user);
}
