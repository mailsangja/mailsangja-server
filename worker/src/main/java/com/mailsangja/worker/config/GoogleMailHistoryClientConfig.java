package com.mailsangja.worker.config;

import com.mailsangja.worker.config.properties.GoogleMailHistoryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleMailHistoryClientConfig {

    @Bean
    public RestClient googleMailHistoryRestClient(GoogleMailHistoryProperties googleMailHistoryProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) googleMailHistoryProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) googleMailHistoryProperties.getReadTimeout().toMillis());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
