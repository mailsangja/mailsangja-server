package com.mailsangja.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DbApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbApplication.class, args);
    }
}
