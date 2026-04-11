package com.mailsangja.core.config;

import com.mailsangja.core.config.properties.GoogleMailWatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleMailWatchClientConfig {

    @Bean
    public RestClient googleMailWatchRestClient(GoogleMailWatchProperties googleMailWatchProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) googleMailWatchProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) googleMailWatchProperties.getReadTimeout().toMillis());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
