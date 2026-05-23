package com.mailsangja.db.common.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.redis.rate-limit")
public class MailRateLimitProperties {

    private boolean enabled;
    private long weeklyDraftLimit = 10L;
    private long weeklyReviewLimit = 10L;
    private long weeklySuggestionLimit = 2L;
}
