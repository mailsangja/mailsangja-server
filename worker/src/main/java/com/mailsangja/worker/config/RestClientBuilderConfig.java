package com.mailsangja.worker.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientBuilderConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder restClientBuilder(ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        return RestClient.builder()
                .observationRegistry(observationRegistryProvider.getIfAvailable(() -> ObservationRegistry.NOOP));
    }
}
