package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.fcm")
public class FcmProperties {

    private String serviceAccountKeyPath;
    private String logoImageUrl;
    private String threadDetailUrlTemplate;
}
