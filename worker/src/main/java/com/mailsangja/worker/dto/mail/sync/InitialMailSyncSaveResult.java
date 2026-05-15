package com.mailsangja.worker.dto.mail.sync;

import java.util.List;
import java.util.UUID;

public record InitialMailSyncSaveResult(
        List<UUID> threadIds,
        List<UUID> messageIds
) {
    public InitialMailSyncSaveResult {
        threadIds = threadIds == null ? List.of() : List.copyOf(threadIds);
        messageIds = messageIds == null ? List.of() : List.copyOf(messageIds);
    }
}
