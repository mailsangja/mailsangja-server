package com.mailsangja.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@EntityScan(basePackages = "com.mailsangja.db.entity")
@EnableJpaRepositories(basePackages = "com.mailsangja.db.repository")
@SpringBootApplication(scanBasePackages = {"com.mailsangja.core", "com.mailsangja.db"})
public class CoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }
}
