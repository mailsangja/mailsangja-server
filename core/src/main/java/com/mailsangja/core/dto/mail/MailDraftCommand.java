package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

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
        String model,
        MailDraftPurpose purpose,
        Map<String, String> restoreTokenMap
) {

    public MailDraftCommand {
        validateId(userId);
        validateId(mailAccountId);
        validateText(maskedQuery);
        validatePurpose(purpose);
        to = nullToEmpty(to);
        cc = nullToEmpty(cc);
        restoreTokenMap = nullToEmpty(restoreTokenMap);
    }

    public MailDraftCommand(UUID userId, UUID mailAccountId, String maskedQuery, UUID replyMessageId, List<String> to, List<String> cc) {
        this(userId, mailAccountId, maskedQuery, replyMessageId, to, cc, null, purposeOf(replyMessageId), Map.of());
    }

    public MailDraftCommand(UUID userId, UUID mailAccountId, String maskedQuery, UUID replyMessageId,
                            List<String> to, List<String> cc, MailDraftPurpose purpose,
                            Map<String, String> restoreTokenMap) {
        this(userId, mailAccountId, maskedQuery, replyMessageId, to, cc, null, purpose, restoreTokenMap);
    }

    public static MailDraftCommand from(UUID userId, UUID mailAccountId, MailDraftStreamRequest request) {
        return new MailDraftCommand(
                userId,
                mailAccountId,
                request.query(),
                request.replyMessageId(),
                request.to(),
                request.cc(),
                request.model(),
                purposeOf(request.replyMessageId()),
                Map.of()
        );
    }

    public static MailDraftCommand of(UUID userId, UUID mailAccountId, MailDraftStreamRequest request,
                                      MailDraftMaskedContextResult maskedContext) {
        return new MailDraftCommand(
                userId,
                mailAccountId,
                maskedContext.maskedQuery(),
                request.replyMessageId(),
                maskedContext.maskedTo(),
                maskedContext.maskedCc(),
                request.model(),
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

    private static void validateId(UUID id) {
        if (id == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateText(String value) {
        if (value == null || value.isBlank()) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static void validatePurpose(MailDraftPurpose purpose) {
        if (purpose == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static List<String> nullToEmpty(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private static Map<String, String> nullToEmpty(Map<String, String> values) {
        if (values == null) {
            return Map.of();
        }
        return Map.copyOf(values);
    }
}
