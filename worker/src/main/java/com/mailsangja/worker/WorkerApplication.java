package com.mailsangja.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableRabbit
@EnableJpaAuditing
@EntityScan(basePackages = "com.mailsangja.db")
@EnableJpaRepositories(basePackages = "com.mailsangja.db")
@SpringBootApplication(scanBasePackages = {"com.mailsangja.worker", "com.mailsangja.db"})
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
