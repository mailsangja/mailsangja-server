package com.mailsangja.core.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청")
public record RegisterRequest(
        @Schema(description = "사용자 이름", example = "홍길동")
        String name,

        @Schema(description = "로그인 아이디", example = "hong@mailsangja.com")
        String username,

        @Schema(description = "비밀번호 (8자 이상)", example = "password123!")
        String password
) {
}
