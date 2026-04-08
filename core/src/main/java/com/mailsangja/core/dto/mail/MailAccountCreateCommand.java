package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.MailProvider;

import java.time.LocalDateTime;

public record MailAccountCreateCommand(
        MailProvider provider,
        String emailAddress,
        String alias,
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
            String alias,
            String icon,
            String color,
            String syncHistoryId
    ) {
        return new MailAccountCreateCommand(
                provider,
                result.emailAddress(),
                alias,
                icon,
                color,
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                syncHistoryId
        );
    }
}
