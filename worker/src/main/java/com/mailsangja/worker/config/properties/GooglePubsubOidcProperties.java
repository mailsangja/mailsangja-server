package com.mailsangja.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.gmail.push.oidc")
public class GooglePubsubOidcProperties {

    private String jwkSetUri;
    private List<String> allowedIssuers;
    private String audience;
    private String allowedServiceAccountEmail;
}
