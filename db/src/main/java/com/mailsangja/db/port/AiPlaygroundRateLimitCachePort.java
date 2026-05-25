package com.mailsangja.db.port;

import java.util.UUID;

public interface AiPlaygroundRateLimitCachePort {

    boolean tryConsumeWeeklyLimit(UUID userId);
}
