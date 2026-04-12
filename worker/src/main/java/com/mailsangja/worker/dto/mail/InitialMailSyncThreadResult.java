package com.mailsangja.worker.dto.mail;

import java.util.List;

public record InitialMailSyncThreadResult(
        String gmailThreadId,
        String historyId,
        List<InitialMailSyncMessageResult> messages
) {
}
