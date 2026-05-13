package com.mailsangja.core.dto.mail;

import java.util.Map;

public record MailDraftRestoreContextResult(Map<String, String> tokens) {

    public MailDraftRestoreContextResult {
        tokens = nullToEmpty(tokens);
    }

    public static MailDraftRestoreContextResult from(MailDraftCommand command) {
        return new MailDraftRestoreContextResult(command.restoreTokenMap());
    }

    private static Map<String, String> nullToEmpty(Map<String, String> tokens) {
        if (tokens == null) {
            return Map.of();
        }
        return Map.copyOf(tokens);
    }
}
