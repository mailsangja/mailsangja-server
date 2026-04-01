package com.mailsangja.core.dto.mail;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleOAuthTokenResult(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("expires_in")
        Long expiresIn,
        @JsonProperty("scope")
        String scope,
        @JsonProperty("token_type")
        String tokenType
) {
}
