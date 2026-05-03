package com.mailsangja.worker.config;

import com.mailsangja.worker.config.properties.PortOneProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PortOneClientConfig {

    @Bean
    public RestClient portOneRestClient(PortOneProperties portOneProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) portOneProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) portOneProperties.getReadTimeout().toMillis());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
