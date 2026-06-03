package com.mailsangja.core.config;

import com.mailsangja.core.config.properties.GoogleGmailApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleGmailApiClient {

    @Bean
    @Qualifier("googleGmailApiRestClient")
    public RestClient googleGmailApiRestClient(
            GoogleGmailApiProperties googleGmailApiProperties,
            RestClient.Builder restClientBuilder
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) googleGmailApiProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) googleGmailApiProperties.getReadTimeout().toMillis());

        return restClientBuilder
                .baseUrl(googleGmailApiProperties.getBaseUri())
                .requestFactory(requestFactory)
                .build();
    }
}
