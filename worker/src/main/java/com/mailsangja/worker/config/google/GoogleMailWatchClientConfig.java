package com.mailsangja.worker.config.google;

import com.mailsangja.worker.config.properties.GoogleMailWatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleMailWatchClientConfig {

    @Bean
    public RestClient googleMailWatchRestClient(
            GoogleMailWatchProperties googleMailWatchProperties,
            RestClient.Builder restClientBuilder
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) googleMailWatchProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) googleMailWatchProperties.getReadTimeout().toMillis());

        return restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }
}
