package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GooglePubsubOidcProperties;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GooglePubsubOidcApiService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String EMAIL_CLAIM = "email";

    private final GooglePubsubOidcProperties googlePubsubOidcProperties;
    private final NimbusJwtDecoder jwtDecoder;

    public GooglePubsubOidcApiService(GooglePubsubOidcProperties googlePubsubOidcProperties) {
        this.googlePubsubOidcProperties = googlePubsubOidcProperties;
        validateProperties(googlePubsubOidcProperties);

        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(googlePubsubOidcProperties.getJwkSetUri()).build();
        this.jwtDecoder.setJwtValidator(buildJwtValidator(googlePubsubOidcProperties));
    }

    public void validateAuthorization(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);

        try {
            Jwt jwt = jwtDecoder.decode(token);
            validateAuthorizedPrincipal(jwt);
        } catch (MailPushException e) {
            throw e;
        } catch (Exception e) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_OIDC_TOKEN);
        }
    }

    private OAuth2TokenValidator<Jwt> buildJwtValidator(GooglePubsubOidcProperties properties) {
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefault();

        return jwt -> {
            OAuth2TokenValidatorResult defaultResult = defaultValidator.validate(jwt);
            if (defaultResult.hasErrors()) {
                return defaultResult;
            }

            String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
            if (isBlank(issuer) || !properties.getAllowedIssuers().contains(issuer)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Invalid issuer", null)
                );
            }

            List<String> audiences = jwt.getAudience();
            if (audiences == null || !audiences.contains(properties.getAudience())) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Invalid audience", null)
                );
            }

            return OAuth2TokenValidatorResult.success();
        };
    }

    private void validateAuthorizedPrincipal(Jwt jwt) {
        String allowedServiceAccountEmail = googlePubsubOidcProperties.getAllowedServiceAccountEmail();
        if (isBlank(allowedServiceAccountEmail)) {
            return;
        }

        String tokenEmail = jwt.getClaimAsString(EMAIL_CLAIM);
        if (isBlank(tokenEmail) || !allowedServiceAccountEmail.equals(tokenEmail)) {
            throw new MailPushException(MailPushErrorCode.UNAUTHORIZED_PUBSUB_OIDC_TOKEN);
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (isBlank(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_AUTHORIZATION_HEADER);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (isBlank(token)) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_AUTHORIZATION_HEADER);
        }

        return token;
    }

    private void validateProperties(GooglePubsubOidcProperties properties) {
        if (isBlank(properties.getJwkSetUri())
                || isBlank(properties.getAudience())
                || properties.getAllowedIssuers() == null
                || properties.getAllowedIssuers().isEmpty()
                || properties.getAllowedIssuers().stream().anyMatch(this::isBlank)) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_OIDC_TOKEN);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
