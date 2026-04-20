package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleOAuthProperties;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GoogleOAuthQueryService 테스트")
class GoogleOAuthQueryServiceTest {

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("유효한 응답이면 access token 결과를 반환한다")
        void refreshAccessToken_유효한응답이면AccessToken결과를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "access_token": "new-access-token",
                              "expires_in": 3600,
                              "token_type": "Bearer"
                            }
                            """)).build()
            );

            // when
            GoogleOAuthTokenResult result = service.refreshAccessToken("refresh-token");

            // then
            assertEquals("new-access-token", result.accessToken());
            assertEquals(3600L, result.expiresIn());
        }

        @Test
        @DisplayName("refresh token이 비어 있으면 예외를 반환한다")
        void refreshAccessToken_refreshToken이비어있으면예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().build()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.refreshAccessToken(" ")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-REFRESH-FAILED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("응답에 access token이 없으면 예외를 반환한다")
        void refreshAccessToken_응답에AccessToken이없으면예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "expires_in": 3600,
                              "token_type": "Bearer"
                            }
                            """)).build()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.refreshAccessToken("refresh-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-REFRESH-FAILED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("RestClient 예외가 발생하면 예외를 반환한다")
        void refreshAccessToken_restClient예외가발생하면예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.refreshAccessToken("refresh-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-REFRESH-FAILED", exception.getErrorCode().getCode());
        }
    }

    private GoogleOAuthProperties createProperties() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setTokenUri("https://oauth2.googleapis.com/token");
        return properties;
    }

    private static final class StubClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        private final String responseBody;

        private StubClientHttpRequestFactory(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    MockClientHttpResponse response = new MockClientHttpResponse(responseBody.getBytes(), HttpStatus.OK);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }
            };
        }
    }

    private static final class FailingClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    throw new ResourceAccessException("failed");
                }
            };
        }
    }
}
