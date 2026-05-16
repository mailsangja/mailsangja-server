package com.mailsangja.db.port;

import java.util.UUID;

public interface LabelSuggestionRateLimitCachePort {

    boolean tryConsumeMonthlyLimit(UUID userId);
}
