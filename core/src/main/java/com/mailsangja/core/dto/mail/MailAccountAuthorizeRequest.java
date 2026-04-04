package com.mailsangja.core.dto.mail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메일 계정 연동 시작 요청")
public record MailAccountAuthorizeRequest(
        @Schema(description = "사용자가 지정한 메일 계정 별칭", example = "업무 메일")
        String alias,
        @Schema(description = "클라이언트가 선택한 메일 계정 아이콘", example = "mail")
        String icon,
        @Schema(description = "클라이언트가 선택한 메일 계정 색상 HEX 값", example = "#4F46E5")
        String color
) {
}
