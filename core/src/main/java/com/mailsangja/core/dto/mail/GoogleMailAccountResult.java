package com.mailsangja.core.dto.mail;

import java.time.LocalDateTime;

public record GoogleMailAccountResult(
        String emailAddress,
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken
) {
}
