package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

import java.util.List;
import java.util.Map;

public record MailDraftMaskedContextResult(
        String maskedQuery,
        List<String> maskedTo,
        List<String> maskedCc,
        Map<String, String> restoreTokenMap
) {

    public MailDraftMaskedContextResult {
        validateText(maskedQuery);
        maskedTo = nullToEmpty(maskedTo);
        maskedCc = nullToEmpty(maskedCc);
        restoreTokenMap = nullToEmpty(restoreTokenMap);
    }

    private static void validateText(String value) {
        if (value == null || value.isBlank()) {
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
