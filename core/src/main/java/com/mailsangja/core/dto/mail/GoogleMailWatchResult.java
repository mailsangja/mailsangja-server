package com.mailsangja.core.dto.mail;

import java.time.LocalDateTime;

public record GoogleMailWatchResult(
        String historyId,
        LocalDateTime expirationAt
) {
}
