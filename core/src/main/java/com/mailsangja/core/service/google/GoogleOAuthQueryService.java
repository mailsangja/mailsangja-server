package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleOAuthProperties;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.core.dto.mail.GoogleUserInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class GoogleOAuthQueryService {

    private final GoogleOAuthProperties googleOAuthProperties;
    private final RestClient googleOAuthRestClient;

    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
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
    }

    public GoogleOAuthTokenResult exchangeCodeForToken(String code) {
        validateTokenExchangeInput(code);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", googleOAuthProperties.getClientId());
        formData.add("client_secret", googleOAuthProperties.getClientSecret());
        formData.add("redirect_uri", googleOAuthProperties.getRedirectUri());
        formData.add("grant_type", "authorization_code");

        try {
            GoogleOAuthTokenResult tokenResult = googleOAuthRestClient
                    .post()
                    .uri(googleOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(formData)
                    .retrieve()
                    .body(GoogleOAuthTokenResult.class);

            validateTokenResult(tokenResult);
            return tokenResult;
        } catch (RestClientException e) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
        }
    }

    public GoogleUserInfoResult fetchUserInfo(String accessToken) {
        validateUserInfoInput(accessToken);

        try {
            GoogleUserInfoResult userInfoResult = googleOAuthRestClient
                    .get()
                    .uri(googleOAuthProperties.getUserInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleUserInfoResult.class);

            validateUserInfoResult(userInfoResult);
            return userInfoResult;
        } catch (RestClientException e) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_USER_INFO_FETCH_FAILED);
        }
    }

    public GoogleMailAccountResult getGoogleMailAccountResult(String code) {
        GoogleOAuthTokenResult tokenResult = exchangeCodeForToken(code);
        GoogleUserInfoResult userInfoResult = fetchUserInfo(tokenResult.accessToken());

        return new GoogleMailAccountResult(
                userInfoResult.email(),
                tokenResult.accessToken(),
                LocalDateTime.now().plusSeconds(tokenResult.expiresIn()),
                tokenResult.refreshToken()
        );
    }

    private void validateTokenExchangeInput(String code) {
        if (isBlank(code)
                || isBlank(googleOAuthProperties.getClientId())
                || isBlank(googleOAuthProperties.getClientSecret())
                || isBlank(googleOAuthProperties.getRedirectUri())
                || isBlank(googleOAuthProperties.getTokenUri())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
        }
    }

    private void validateTokenResult(GoogleOAuthTokenResult tokenResult) {
        if (tokenResult == null
                || isBlank(tokenResult.accessToken())
                || tokenResult.expiresIn() == null
                || tokenResult.expiresIn() <= 0) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
        }
    }

    private void validateUserInfoInput(String accessToken) {
        if (isBlank(accessToken) || isBlank(googleOAuthProperties.getUserInfoUri())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_USER_INFO_FETCH_FAILED);
        }
    }

    private void validateUserInfoResult(GoogleUserInfoResult userInfoResult) {
        if (userInfoResult == null || isBlank(userInfoResult.email())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_USER_INFO_FETCH_FAILED);
        }

        if (!Boolean.TRUE.equals(userInfoResult.verifiedEmail())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
        }
    }

    private String buildScopeValue() {
        validateScopes();

        StringJoiner scopeJoiner = new StringJoiner(" ");
        for (String scope : googleOAuthProperties.getScopes()) {
            scopeJoiner.add(scope);
        }
        return scopeJoiner.toString();
    }

    private void validateScopes() {
        if (googleOAuthProperties.getScopes() == null || googleOAuthProperties.getScopes().isEmpty()) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
