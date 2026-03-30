package com.mailsangja.core.dto.auth;

public record RegisterRequest(
        String name,
        String username,
        String password
) {
}
