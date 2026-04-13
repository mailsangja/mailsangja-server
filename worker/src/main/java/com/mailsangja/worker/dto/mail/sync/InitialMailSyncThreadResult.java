package com.mailsangja.worker.dto.mail.sync;

import java.util.List;

public record InitialMailSyncThreadResult(
        String gmailThreadId,
        String historyId,
        List<InitialMailSyncMessageResult> messages
) {
}
