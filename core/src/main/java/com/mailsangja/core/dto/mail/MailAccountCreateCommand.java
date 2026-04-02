package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.MailProvider;

import java.time.LocalDateTime;

public record MailAccountCreateCommand(
        MailProvider provider,
        String emailAddress,
        String icon,
        String color,
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        String syncHistoryId
) {
    public static MailAccountCreateCommand from(
            MailProvider provider,
            GoogleMailAccountResult result,
            String icon,
            String color
    ) {
        return new MailAccountCreateCommand(
                provider,
                result.emailAddress(),
                icon,
                color,
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                null
        );
    }
}
