package com.mailsangja.core.dto.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "주소록 생성 요청")
public record ContactCreateRequest(
        @NotBlank
        @Schema(description = "연락처 이름", example = "Alice")
        String name,

        @Email
        @NotBlank
        @Schema(description = "연락처 이메일", example = "alice@example.com")
        String email
) {
}
