package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.MailProvider;

import java.time.LocalDateTime;

public record MailAccountCreateCommand(
        MailProvider provider,
        String emailAddress,
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        String syncHistoryId
) {
}
