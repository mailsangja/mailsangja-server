package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.user.User;

import java.util.UUID;

public record MailSendCommand(
        UUID userId,
        String composeSessionId,
        String from
) {

    public static MailSendCommand from(User user, MailSendRequest request) {
        return new MailSendCommand(
                user.getId(),
                request.composeSessionId(),
                request.from()
        );
    }
}
