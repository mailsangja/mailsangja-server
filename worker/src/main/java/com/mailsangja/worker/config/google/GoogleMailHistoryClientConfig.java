package com.mailsangja.worker.config.google;

import com.mailsangja.worker.config.properties.GoogleMailHistoryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleMailHistoryClientConfig {

    @Bean
    public RestClient googleMailHistoryRestClient(
            GoogleMailHistoryProperties googleMailHistoryProperties,
            RestClient.Builder restClientBuilder
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) googleMailHistoryProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) googleMailHistoryProperties.getReadTimeout().toMillis());

        return restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }
}
