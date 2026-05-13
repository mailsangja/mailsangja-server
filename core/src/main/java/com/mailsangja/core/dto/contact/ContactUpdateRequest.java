package com.mailsangja.core.dto.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "주소록 수정 요청")
public record ContactUpdateRequest(
        @NotBlank
        @Schema(description = "연락처 이름", example = "Alice Updated")
        String name
) {
}
