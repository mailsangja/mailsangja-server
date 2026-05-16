package com.mailsangja.db.common.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.redis.rate-limit")
public class MailDraftRateLimitProperties {

    private boolean enabled;
    private long monthlyLimit = 50L;
    private long reviewMonthlyLimit = 50L;
}
