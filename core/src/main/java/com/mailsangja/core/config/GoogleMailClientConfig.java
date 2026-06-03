package com.mailsangja.core.config;

import com.mailsangja.core.config.properties.GoogleMailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleMailClientConfig {

    @Bean
    public RestClient googleMailRestClient(
            GoogleMailProperties googleMailProperties,
            RestClient.Builder restClientBuilder
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) googleMailProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) googleMailProperties.getReadTimeout().toMillis());

        return restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }
}
