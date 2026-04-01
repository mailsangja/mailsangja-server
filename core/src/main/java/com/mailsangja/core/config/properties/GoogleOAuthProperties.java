package com.mailsangja.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.oauth.google")
public class GoogleOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private List<String> scopes;
}
