package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleOAuthProperties;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GoogleOAuthApiService {

    private final GoogleOAuthProperties googleOAuthProperties;
    private final RestClient googleOAuthRestClient;

    public GoogleOAuthApiService(
            GoogleOAuthProperties googleOAuthProperties,
            @Qualifier("googleOAuthRestClient") RestClient googleOAuthRestClient
    ) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.googleOAuthRestClient = googleOAuthRestClient;
    }

    public GoogleOAuthTokenResult refreshAccessToken(String refreshToken) {
        validateRefreshInput(refreshToken);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", googleOAuthProperties.getClientId());
        formData.add("client_secret", googleOAuthProperties.getClientSecret());
        formData.add("refresh_token", refreshToken);
        formData.add("grant_type", "refresh_token");

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
            throw new MailPushException(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    private void validateRefreshInput(String refreshToken) {
        if (isBlank(refreshToken)
                || isBlank(googleOAuthProperties.getClientId())
                || isBlank(googleOAuthProperties.getClientSecret())
                || isBlank(googleOAuthProperties.getTokenUri())) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    private void validateTokenResult(GoogleOAuthTokenResult tokenResult) {
        if (tokenResult == null
                || isBlank(tokenResult.accessToken())
                || tokenResult.expiresIn() == null
                || tokenResult.expiresIn() <= 0) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
