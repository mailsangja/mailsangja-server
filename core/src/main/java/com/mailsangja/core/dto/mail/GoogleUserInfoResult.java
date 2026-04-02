package com.mailsangja.core.dto.mail;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfoResult(
        @JsonProperty("email")
        String email,
        @JsonProperty("verified_email")
        Boolean verifiedEmail
) {
}
