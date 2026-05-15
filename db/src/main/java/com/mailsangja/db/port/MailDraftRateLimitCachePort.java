package com.mailsangja.db.port;

import java.util.UUID;

public interface MailDraftRateLimitCachePort {

    boolean tryConsumeMonthlyLimit(UUID userId);
}
