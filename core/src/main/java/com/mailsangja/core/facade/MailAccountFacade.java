package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleOAuthProperties;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.core.service.mail.MailAccountCommandService;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.StringJoiner;

@Component
@RequiredArgsConstructor
public class MailAccountFacade {

    private final MailAccountCommandService mailAccountCommandService;
    private final GoogleOAuthProperties googleOAuthProperties;

    public MailAccountAuthorizeResponse authorizeGoogle(String state) {
        String authorizationUrl = UriComponentsBuilder
                .fromUriString(googleOAuthProperties.getAuthorizationUri())
                .queryParam("client_id", googleOAuthProperties.getClientId())
                .queryParam("redirect_uri", googleOAuthProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", buildScopeValue())
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .toUriString();
        return new MailAccountAuthorizeResponse(authorizationUrl);
    }

    public MailAccountResponse handleGoogleCallback(User user, String code) {
        validateAuthorizationCode(code);

        GoogleMailAccountResult result = createStubGoogleMailAccountResult(code);
        return MailAccountResponse.from(mailAccountCommandService.createGoogleMailAccount(user, result));
    }

    private GoogleMailAccountResult createStubGoogleMailAccountResult(String code) {
        String normalizedCode = code == null ? "unknown" : code;
        return new GoogleMailAccountResult(
                "stub-" + normalizedCode + "@gmail.com",
                "stub-access-token",
                LocalDateTime.now().plusHours(1),
                "stub-refresh-token"
        );
    }

    private void validateAuthorizationCode(String code) {
        if (isBlank(code) || code.contains(" ") || code.length() > 2048) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_AUTHORIZATION_CODE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildScopeValue() {
        StringJoiner scopeJoiner = new StringJoiner(" ");
        for (String scope : googleOAuthProperties.getScopes()) {
            scopeJoiner.add(scope);
        }
        return scopeJoiner.toString();
    }
}
