package com.mailsangja.core.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

@Configuration
public class AsyncConfig {

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> parentContext = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previousContext = MDC.getCopyOfContextMap();
                try {
                    if (parentContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(parentContext);
                    }
                    runnable.run();
                } finally {
                    if (previousContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previousContext);
                    }
                }
            };
        };
    }
}
