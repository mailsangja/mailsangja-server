package com.mailsangja.core.dto.mail;

import java.util.List;
import java.util.UUID;

public record MailDraftCommand(
        UUID userId,
        UUID mailAccountId,
        String maskedQuery,
        UUID replyMessageId,
        List<String> to,
        List<String> cc
) {

    public static MailDraftCommand from(UUID userId, MailDraftStreamRequest request) {
        return new MailDraftCommand(
                userId,
                request.mailAccountId(),
                request.query(),
                request.replyMessageId(),
                request.to(),
                request.cc()
        );
    }
}
