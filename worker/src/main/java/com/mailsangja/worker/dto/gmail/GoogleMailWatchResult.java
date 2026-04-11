package com.mailsangja.worker.dto.gmail;

import java.time.LocalDateTime;

public record GoogleMailWatchResult(
        String historyId,
        LocalDateTime expirationAt
) {
}
