package com.mailsangja.core.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "로그인 아이디", example = "hong@mailsangja.com")
        String username,

        @Schema(description = "비밀번호", example = "password123!")
        String password
) {
}
