package com.mailsangja.db.port;

import java.util.UUID;

public interface MailReviewRateLimitCachePort {

    boolean tryConsumeMonthlyLimit(UUID userId);
}
