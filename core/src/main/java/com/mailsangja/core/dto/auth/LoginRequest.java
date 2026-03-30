package com.mailsangja.core.dto.auth;

public record LoginRequest(
        String username,
        String password
) {
}
