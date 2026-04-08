package com.mailsangja.worker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("메일상자 Worker API")
                        .description("Gmail Push Webhook 및 워커 처리 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("메일상자 팀")
                                .email("hramst0618@gmail.com")));
    }
}
