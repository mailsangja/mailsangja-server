package com.mailsangja.core.dto.mail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Google OAuth 인가 URL 응답")
public record MailAccountAuthorizeResponse(
        @Schema(
                description = "사용자를 Google 동의 화면으로 이동시키기 위한 인가 URL",
                example = "https://accounts.google.com/o/oauth2/v2/auth?client_id=example-client-id&redirect_uri=http://localhost:8080/api/v1/mail-accounts/google/callback&response_type=code&scope=openid%20email%20https://mail.google.com/&access_type=offline&prompt=consent&state=example-state"
        )
        String authorizationUrl
) {
}
