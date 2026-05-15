package com.mailsangja.db.dto;

import com.mailsangja.db.entity.mail.Direction;

import java.util.UUID;

public record MailDraftReferenceMessageResult(
        UUID messageId,
        UUID mailAccountId,
        Direction direction,
        String subject,
        String body
) {
}
