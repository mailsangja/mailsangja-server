package com.mailsangja.core.dto.mail;

import java.util.Map;

public record MailDraftRestoreContextResult(Map<String, String> tokens) {

    public static MailDraftRestoreContextResult from(MailDraftCommand command) {
        return new MailDraftRestoreContextResult(command.restoreTokenMap());
    }
}
