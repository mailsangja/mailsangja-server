package com.mailsangja.core.dto.mail;

import java.util.List;
import java.util.Map;

public record MailDraftMaskedContextResult(
        String maskedQuery,
        List<String> maskedTo,
        List<String> maskedCc,
        Map<String, String> restoreTokenMap
) {
}
