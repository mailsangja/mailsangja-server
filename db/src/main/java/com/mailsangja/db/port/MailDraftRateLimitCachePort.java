package com.mailsangja.db.port;

import java.util.UUID;

public interface MailDraftRateLimitCachePort {

    long incrementMonthlyCount(UUID userId);
}
