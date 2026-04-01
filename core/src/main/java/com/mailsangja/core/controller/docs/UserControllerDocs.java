package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.auth.UserInfoResponse;
import com.mailsangja.core.dto.user.UpdateDefaultAccountRequest;
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

@Tag(name = "User", description = "사용자 설정 API")
public interface UserControllerDocs {

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
    ResponseEntity<UserInfoResponse> getUserInfo(@Parameter(hidden = true) @AuthUser User user);

    @Operation(
            summary = "기본 발신 메일 계정 수정",
            description = "현재 로그인된 사용자의 기본 발신 메일 계정을 변경합니다. SESSION 쿠키가 필요합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "기본 발신 메일 계정 수정 성공",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "메일 계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 (로그인되지 않은 상태)",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<Void> updateDefaultAccount(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody(
                    description = "기본 발신 메일 계정 수정 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateDefaultAccountRequest.class))
            )
            UpdateDefaultAccountRequest request
    );
}
