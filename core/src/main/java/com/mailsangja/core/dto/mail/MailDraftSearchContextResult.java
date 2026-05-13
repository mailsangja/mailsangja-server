package com.mailsangja.core.dto.mail;

import java.util.UUID;

public record MailDraftSearchContextResult(
        UUID messageId,
        String source,
        String subject,
        String body
) {
}
