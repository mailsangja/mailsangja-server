package com.mailsangja.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.gmail.api")
public class GoogleGmailApiProperties {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    private String baseUri;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
}
