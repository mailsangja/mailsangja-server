package com.mailsangja.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.google.people")
public class GooglePeopleProperties {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    private String connectionsUri;
    private int pageSize = 1000;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
}
