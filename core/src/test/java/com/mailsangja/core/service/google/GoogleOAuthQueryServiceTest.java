package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleOAuthProperties;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.core.dto.mail.GoogleUserInfoResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GoogleOAuthQueryService 테스트")
class GoogleOAuthQueryServiceTest {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Nested
    @DisplayName("인가 URL 생성")
    class BuildAuthorizationUrl {

        @Test
        @DisplayName("필수 파라미터와 scope를 포함한 URL을 생성한다")
        void buildAuthorizationUrl_필수파라미터와Scope를포함한Url을생성한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().build()
            );

            // when
            String authorizationUrl = service.buildAuthorizationUrl("state-1");

            // then
            assertTrue(authorizationUrl.contains("client_id=client-id"));
            assertTrue(authorizationUrl.contains("redirect_uri="));
            assertTrue(authorizationUrl.contains("scope="));
            assertTrue(authorizationUrl.contains("scope1"));
            assertTrue(authorizationUrl.contains("scope2"));
            assertTrue(authorizationUrl.contains("state=state-1"));
        }

        @Test
        @DisplayName("scope가 비어 있으면 토큰 교환 실패 예외를 반환한다")
        void buildAuthorizationUrl_scope가비어있으면토큰교환실패예외를반환한다() {
            // given
            GoogleOAuthProperties properties = createProperties();
            properties.setScopes(List.of());
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(properties, RestClient.builder().build());

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.buildAuthorizationUrl("state-1")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-EXCHANGE-FAILED", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("토큰 교환")
    class ExchangeCodeForToken {

        @Test
        @DisplayName("유효한 응답이면 액세스 토큰 결과를 반환한다")
        void exchangeCodeForToken_유효한응답이면액세스토큰결과를반환한다() {
            // given
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory(
                    """
                            {"access_token":"access-token","refresh_token":"refresh-token","expires_in":3600,"scope":"scope1 scope2","token_type":"Bearer"}
                            """,
                    """
                            {"email":"user@example.com","verified_email":true}
                            """
            );
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            GoogleOAuthTokenResult result = service.exchangeCodeForToken("auth-code");

            // then
            assertEquals("access-token", result.accessToken());
            assertEquals("refresh-token", result.refreshToken());
            assertTrue(requestFactory.requestBody().contains("code=auth-code"));
            assertTrue(requestFactory.requestBody().contains("grant_type=authorization_code"));
        }

        @Test
        @DisplayName("인가 코드가 비어 있으면 토큰 교환 실패 예외를 반환한다")
        void exchangeCodeForToken_인가코드가비어있으면토큰교환실패예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(createProperties(), RestClient.builder().build());

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.exchangeCodeForToken(" ")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-EXCHANGE-FAILED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("응답에 액세스 토큰이 없으면 토큰 교환 실패 예외를 반환한다")
        void exchangeCodeForToken_응답에액세스토큰이없으면토큰교환실패예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(new CapturingClientHttpRequestFactory(
                            """
                                    {"access_token":" ","refresh_token":"refresh-token","expires_in":3600,"scope":"scope1 scope2","token_type":"Bearer"}
                                    """,
                            """
                                    {"email":"user@example.com","verified_email":true}
                                    """
                    )).build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.exchangeCodeForToken("auth-code")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-EXCHANGE-FAILED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("RestClient 호출이 실패하면 토큰 교환 실패 예외를 반환한다")
        void exchangeCodeForToken_restClient호출이실패하면토큰교환실패예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.exchangeCodeForToken("auth-code")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-EXCHANGE-FAILED", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class RefreshAccessToken {

        @Test
        @DisplayName("리프레시 토큰으로 새 액세스 토큰을 조회한다")
        void refreshAccessToken_리프레시토큰으로새액세스토큰을조회한다() {
            // given
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory(
                    """
                            {"access_token":"new-access-token","refresh_token":"new-refresh-token","expires_in":3600,"scope":"scope1 scope2","token_type":"Bearer"}
                            """,
                    """
                            {"email":"user@example.com","verified_email":true}
                            """
            );
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            GoogleOAuthTokenResult result = service.refreshAccessToken("refresh-token");

            // then
            assertEquals("new-access-token", result.accessToken());
            assertTrue(requestFactory.requestBody().contains("refresh_token=refresh-token"));
            assertTrue(requestFactory.requestBody().contains("grant_type=refresh_token"));
        }

        @Test
        @DisplayName("리프레시 토큰이 비어 있으면 갱신 실패 예외를 반환한다")
        void refreshAccessToken_리프레시토큰이비어있으면갱신실패예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(createProperties(), RestClient.builder().build());

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.refreshAccessToken(" ")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-TOKEN-REFRESH-FAILED", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("사용자 정보 조회")
    class FetchUserInfo {

        @Test
        @DisplayName("검증된 이메일이면 사용자 정보를 반환한다")
        void fetchUserInfo_검증된이메일이면사용자정보를반환한다() {
            // given
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory(
                    """
                            {"access_token":"access-token","refresh_token":"refresh-token","expires_in":3600,"scope":"scope1 scope2","token_type":"Bearer"}
                            """,
                    """
                            {"email":"user@example.com","verified_email":true}
                            """
            );
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            GoogleUserInfoResult result = service.fetchUserInfo("access-token");

            // then
            assertEquals("user@example.com", result.email());
            assertEquals("Bearer access-token", requestFactory.authorizationHeader());
        }

        @Test
        @DisplayName("이메일 검증이 안 되어 있으면 예외를 반환한다")
        void fetchUserInfo_이메일검증이안되어있으면예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(new CapturingClientHttpRequestFactory(
                            """
                                    {"access_token":"access-token","refresh_token":"refresh-token","expires_in":3600,"scope":"scope1 scope2","token_type":"Bearer"}
                                    """,
                            """
                                    {"email":"user@example.com","verified_email":false}
                                    """
                    )).build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.fetchUserInfo("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-EMAIL-NOT-VERIFIED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("사용자 정보 응답에 이메일이 없으면 예외를 반환한다")
        void fetchUserInfo_사용자정보응답에이메일이없으면예외를반환한다() {
            // given
            GoogleOAuthQueryService service = new GoogleOAuthQueryService(
                    createProperties(),
                    RestClient.builder().requestFactory(new CapturingClientHttpRequestFactory(
                            """
                                    {"access_token":"access-token","refresh_token":"refresh-token","expires_in":3600,"scope":"scope1 scope2","token_type":"Bearer"}
                                    """,
                            """
                                    {"email":" ","verified_email":true}
                                    """
                    )).build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.fetchUserInfo("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-USER-INFO-FETCH-FAILED", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("구글 메일 계정 정보 조회")
    class GetGoogleMailAccountResult {

        @Test
        @DisplayName("토큰 만료 시각을 KST 기준으로 계산한다")
        void getGoogleMailAccountResult_토큰만료시각을Kst기준으로계산한다() {
            // given
            GoogleOAuthQueryService service = new FakeGoogleOAuthQueryService();
            LocalDateTime lowerBound = LocalDateTime.now(KST_ZONE_ID).plusSeconds(3600);

            // when
            GoogleMailAccountResult result = service.getGoogleMailAccountResult("auth-code");

            // then
            LocalDateTime upperBound = LocalDateTime.now(KST_ZONE_ID).plusSeconds(3600);
            assertEquals("user@example.com", result.emailAddress());
            assertEquals("access-token", result.accessToken());
            assertEquals("refresh-token", result.refreshToken());
            assertTrue(!result.accessTokenExpiresAt().isBefore(lowerBound));
            assertTrue(!result.accessTokenExpiresAt().isAfter(upperBound));
        }
    }

    private GoogleOAuthProperties createProperties() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRedirectUri("https://mailsangja.com/oauth/callback");
        properties.setAuthorizationUri("https://accounts.google.com/o/oauth2/auth");
        properties.setTokenUri("https://oauth2.googleapis.com/token");
        properties.setUserInfoUri("https://www.googleapis.com/oauth2/v2/userinfo");
        properties.setScopes(List.of("scope1", "scope2"));
        return properties;
    }

    private static final class CapturingClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        private final String tokenResponseBody;
        private final String userInfoResponseBody;
        private String requestBody;
        private String authorizationHeader;

        private CapturingClientHttpRequestFactory(String tokenResponseBody, String userInfoResponseBody) {
            this.tokenResponseBody = tokenResponseBody;
            this.userInfoResponseBody = userInfoResponseBody;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    requestBody = getBodyAsString();
                    authorizationHeader = getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    String responseBody = uri.toString().contains("userinfo") ? userInfoResponseBody : tokenResponseBody;
                    MockClientHttpResponse response = new MockClientHttpResponse(responseBody.getBytes(), HttpStatus.OK);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }
            };
        }

        private String requestBody() {
            return requestBody;
        }

        private String authorizationHeader() {
            return authorizationHeader;
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

    private static final class FakeGoogleOAuthQueryService extends GoogleOAuthQueryService {

        private FakeGoogleOAuthQueryService() {
            super(new GoogleOAuthProperties(), RestClient.builder().build());
        }

        @Override
        public GoogleOAuthTokenResult exchangeCodeForToken(String code) {
            return new GoogleOAuthTokenResult("access-token", "refresh-token", 3600L, "scope", "Bearer");
        }

        @Override
        public GoogleUserInfoResult fetchUserInfo(String accessToken) {
            return new GoogleUserInfoResult("user@example.com", true);
        }
    }
}
