package com.mailsangja.core.config;

import com.mailsangja.core.config.properties.PortOneProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PortOneClientConfig {

    @Bean
    public RestClient portOneRestClient(PortOneProperties portOneProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(portOneProperties.getConnectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(portOneProperties.getReadTimeout().toMillis()));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
