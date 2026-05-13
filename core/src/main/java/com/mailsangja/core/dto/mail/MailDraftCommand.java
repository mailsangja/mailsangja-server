package com.mailsangja.core.dto.mail;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MailDraftCommand(
        UUID userId,
        UUID mailAccountId,
        String maskedQuery,
        UUID replyMessageId,
        List<String> to,
        List<String> cc,
        MailDraftPurpose purpose,
        Map<String, String> restoreTokenMap
) {

    public MailDraftCommand(UUID userId, UUID mailAccountId, String maskedQuery, UUID replyMessageId, List<String> to, List<String> cc) {
        this(userId, mailAccountId, maskedQuery, replyMessageId, to, cc, purposeOf(replyMessageId), Map.of());
    }

    public static MailDraftCommand from(UUID userId, MailDraftStreamRequest request) {
        return new MailDraftCommand(
                userId,
                request.mailAccountId(),
                request.query(),
                request.replyMessageId(),
                request.to(),
                request.cc(),
                purposeOf(request.replyMessageId()),
                Map.of()
        );
    }

    public static MailDraftCommand of(UUID userId, MailDraftStreamRequest request, MailDraftMaskedContextResult maskedContext) {
        return new MailDraftCommand(
                userId,
                request.mailAccountId(),
                maskedContext.maskedQuery(),
                request.replyMessageId(),
                maskedContext.maskedTo(),
                maskedContext.maskedCc(),
                purposeOf(request.replyMessageId()),
                maskedContext.restoreTokenMap()
        );
    }

    private static MailDraftPurpose purposeOf(UUID replyMessageId) {
        if (replyMessageId == null) {
            return MailDraftPurpose.GENERAL;
        }
        return MailDraftPurpose.REPLY;
    }
}
