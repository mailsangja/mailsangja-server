package com.mailsangja.worker.dto.gmail;

public record GoogleMailWatchResponse(
        String historyId,
        String expiration
) {
}
