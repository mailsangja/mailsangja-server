package com.mailsangja.core.dto.mail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메일 작성 세션 발급 응답")
public record MailComposeResponse(
        @Schema(description = "메일 작성 컨텍스트를 식별하는 compose session ID", example = "8d3e8eb8-2c8a-41d5-beb9-5e9c12d85ddf")
        String composeSessionId
) {
}
