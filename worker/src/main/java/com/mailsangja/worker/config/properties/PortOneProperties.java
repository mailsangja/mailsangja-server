package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.portone")
public class PortOneProperties {

    private String apiSecret;
    private String paymentQueryUri;
    private Duration connectTimeout;
    private Duration readTimeout;
    private Map<String, Integer> planPrices;
}
