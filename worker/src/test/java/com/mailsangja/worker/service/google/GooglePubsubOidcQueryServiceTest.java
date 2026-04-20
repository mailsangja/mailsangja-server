package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GooglePubsubOidcProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GooglePubsubOidcQueryService 테스트")
class GooglePubsubOidcQueryServiceTest {

    @Mock
    private NimbusJwtDecoder jwtDecoder;

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("필수 프로퍼티가 비어 있으면 예외를 반환한다")
        void constructor_필수프로퍼티가비어있으면예외를반환한다() {
            // given
            GooglePubsubOidcProperties properties = new GooglePubsubOidcProperties();
            properties.setAudience("audience");
            properties.setAllowedIssuers(List.of("https://accounts.google.com"));

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> new GooglePubsubOidcQueryService(properties)
            );

            // then
            assertEquals("MS-MAIL-INVALID-PUBSUB-OIDC-TOKEN", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("validateAuthorization")
    class ValidateAuthorization {

        @Test
        @DisplayName("Bearer 형식이 아니면 예외를 반환한다")
        void validateAuthorization_bearer형식이아니면예외를반환한다() {
            // given
            GooglePubsubOidcQueryService service = createService("service-account@test.iam.gserviceaccount.com");

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.validateAuthorization("Basic token")
            );

            // then
            assertEquals("MS-MAIL-INVALID-PUBSUB-AUTHORIZATION-HEADER", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("JWT decode가 실패하면 invalid token 예외를 반환한다")
        void validateAuthorization_jwtDecode가실패하면InvalidToken예외를반환한다() {
            // given
            GooglePubsubOidcQueryService service = createService("service-account@test.iam.gserviceaccount.com");
            given(jwtDecoder.decode("valid-token")).willThrow(new JwtException("decode failed"));

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.validateAuthorization("Bearer valid-token")
            );

            // then
            assertEquals("MS-MAIL-INVALID-PUBSUB-OIDC-TOKEN", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("허용된 서비스 계정 이메일과 다르면 예외를 반환한다")
        void validateAuthorization_허용된서비스계정이메일과다르면예외를반환한다() {
            // given
            GooglePubsubOidcQueryService service = createService("service-account@test.iam.gserviceaccount.com");
            given(jwtDecoder.decode("valid-token")).willReturn(createJwt("other@test.iam.gserviceaccount.com"));

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.validateAuthorization("Bearer valid-token")
            );

            // then
            assertEquals("MS-MAIL-UNAUTHORIZED-PUBSUB-OIDC-TOKEN", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("허용 서비스 계정 제한이 없으면 이메일 클레임이 달라도 통과한다")
        void validateAuthorization_허용서비스계정제한이없으면이메일클레임이달라도통과한다() {
            // given
            GooglePubsubOidcQueryService service = createService(null);
            given(jwtDecoder.decode("valid-token")).willReturn(createJwt("other@test.iam.gserviceaccount.com"));

            // when then
            assertDoesNotThrow(() -> service.validateAuthorization("Bearer valid-token"));
        }
    }

    private GooglePubsubOidcQueryService createService(String allowedServiceAccountEmail) {
        GooglePubsubOidcProperties properties = new GooglePubsubOidcProperties();
        properties.setJwkSetUri("https://www.googleapis.com/oauth2/v3/certs");
        properties.setAudience("mailbox-audience");
        properties.setAllowedIssuers(List.of("https://accounts.google.com"));
        properties.setAllowedServiceAccountEmail(allowedServiceAccountEmail);

        GooglePubsubOidcQueryService service = new GooglePubsubOidcQueryService(properties);
        ReflectionTestUtils.setField(service, "jwtDecoder", jwtDecoder);
        return service;
    }

    private Jwt createJwt(String email) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "iss", "https://accounts.google.com",
                        "aud", List.of("mailbox-audience"),
                        "email", email
                )
        );
    }
}
