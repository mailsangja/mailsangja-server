package com.mailsangja.worker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.mailsangja.worker.config.properties.FcmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FcmConfig {

    @Bean
    public FirebaseApp firebaseApp(FcmProperties fcmProperties, ResourceLoader resourceLoader) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(resolveCredentialsStream(fcmProperties, resourceLoader)))
                .build();

        log.info("Firebase application has been initialized");
        return FirebaseApp.initializeApp(options);
    }

    private InputStream resolveCredentialsStream(FcmProperties fcmProperties, ResourceLoader resourceLoader) throws IOException {
        if (StringUtils.hasText(fcmProperties.getServiceAccountKeyJson())) {
            log.info("Firebase credentials loaded from environment variable");
            return new ByteArrayInputStream(fcmProperties.getServiceAccountKeyJson().getBytes(StandardCharsets.UTF_8));
        }

        String keyPath = fcmProperties.getServiceAccountKeyPath();
        if (!StringUtils.hasText(keyPath)) {
            throw new IllegalStateException(
                    "FCM 인증 정보가 설정되지 않았습니다. " +
                    "MAILSANGJA_FCM_SERVICE_ACCOUNT_KEY_JSON 또는 MAILSANGJA_FCM_SERVICE_ACCOUNT_KEY_PATH를 설정해주세요."
            );
        }

        log.info("Firebase credentials loaded from file path: {}", keyPath);
        return resourceLoader.getResource(keyPath).getInputStream();
    }
}
