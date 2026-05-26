package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.gmail.rate-limit")
public class GmailApiRateLimitProperties {

    private boolean enabled = true;
    private boolean failOpen = true;
    private long perUserCapacityUnits = 12000;
    private long perUserRefillUnitsPerMinute = 12000;
    private Duration keyTtl = Duration.ofMinutes(2);
}
