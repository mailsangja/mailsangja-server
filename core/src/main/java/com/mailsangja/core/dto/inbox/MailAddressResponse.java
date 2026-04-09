package com.mailsangja.core.dto.inbox;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "메일 주소 (이름 + 이메일)")
public record MailAddressResponse(
        @Schema(description = "발신자/수신자 이름 (주소록에 등록된 경우)", example = "김휘래", nullable = true)
        String name,
        @Schema(description = "이메일 주소", example = "hramst0618@gmail.com")
        String email
) {
    public static MailAddressResponse of(String email, Map<String, String> contactNameByEmail) {
        return new MailAddressResponse(contactNameByEmail.get(email), email);
    }
}
