package com.mailsangja.core;

import com.mailsangja.core.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.mockito.Mockito.mock;

@SpringBootTest(classes = CoreApplicationTests.TestApplication.class)
class CoreApplicationTests {

    @Test
    void contextLoads() {
    }

    @SpringBootConfiguration
    @EnableJpaAuditing
    @EntityScan(basePackages = "com.mailsangja.db")
    @EnableJpaRepositories(basePackages = "com.mailsangja.db")
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = {"com.mailsangja.core", "com.mailsangja.db"},
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CoreApplication.class)
            }
    )
    static class TestApplication {

        @Bean
        PasswordEncoder passwordEncoder() {
            return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        }

        @Bean
        AuthenticationManager authenticationManager() {
            return authentication -> authentication;
        }

        @Bean
        SecurityContextRepository securityContextRepository() {
            return new HttpSessionSecurityContextRepository();
        }

        @Bean
        VectorStore vectorStore() {
            return mock(VectorStore.class);
        }
    }

}
