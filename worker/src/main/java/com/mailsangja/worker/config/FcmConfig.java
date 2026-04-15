package com.mailsangja.worker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.mailsangja.worker.config.properties.FcmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Slf4j
@Configuration
public class FcmConfig {

    @Bean
    public FirebaseApp firebaseApp(FcmProperties fcmProperties) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(
                        new ClassPathResource(fcmProperties.getServiceAccountKeyPath()).getInputStream()
                ))
                .build();

        log.info("Firebase application has been initialized");
        return FirebaseApp.initializeApp(options);
    }
}
