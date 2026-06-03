package com.mailsangja.worker.config.google;

import com.mailsangja.worker.config.properties.GoogleOAuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleOAuthClientConfig {

    @Bean
    public RestClient googleOAuthRestClient(
            GoogleOAuthProperties googleOAuthProperties,
            RestClient.Builder restClientBuilder
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) googleOAuthProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) googleOAuthProperties.getReadTimeout().toMillis());

        return restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }
}
