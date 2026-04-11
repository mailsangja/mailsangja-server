package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.gmail.watch-renewal")
public class GmailWatchRenewalProperties {

    private boolean enabled = false;
    private String cron = "0 0 * * * *";
    private Duration renewalWindow = Duration.ofDays(1);
    private int batchSize = 50;
}
