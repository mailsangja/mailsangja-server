package com.mailsangja.worker.dto.gmail.watch;

public record GoogleMailWatchResponse(
        String historyId,
        String expiration
) {
}
