package com.mailsangja.core.dto.mail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메일 주소 정보")
public record MailAddressRequest(
        @Schema(description = "표시 이름", example = "홍길동", nullable = true)
        String name,

        @Schema(description = "이메일 주소", example = "sender@gmail.com")
        String address
) {
}
