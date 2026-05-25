package com.mailsangja.db.port;

import java.util.UUID;

public interface LabelSuggestionRateLimitCachePort {

    boolean tryConsumeWeeklyLimit(UUID userId);

    long getWeeklyUsage(UUID userId);
}
