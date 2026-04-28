package com.mailsangja.worker.config.google;

import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleMailMessageClientConfig {

    @Bean
    public RestClient googleMailMessageRestClient(GoogleMailInitialSyncProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
