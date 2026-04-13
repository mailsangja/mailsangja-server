package com.mailsangja.worker.dto.gmail.watch;

import java.time.LocalDateTime;

public record GoogleMailWatchResult(
        String historyId,
        LocalDateTime expirationAt
) {
}
