package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GoogleMailWatchQueryService 테스트")
class GoogleMailWatchQueryServiceTest {

    @Nested
    @DisplayName("watch 호출")
    class Watch {

        @Test
        @DisplayName("라벨과 필터 값을 정규화해서 watch 결과를 반환한다")
        void watch_라벨과필터값을정규화해서Watch결과를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setWatchUri("https://gmail.googleapis.com/gmail/v1/users/me/watch");
            properties.setTopicName("projects/test/topics/mail");
            properties.setLabelIds(List.of("INBOX", "CATEGORY_PERSONAL"));
            properties.setLabelFilterBehavior(" ");
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory("""
                    {"historyId":"history-1","expiration":"1713240000000"}
                    """);
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(
                    properties,
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            GoogleMailWatchResult result = service.watch("access-token");

            // then
            assertEquals("history-1", result.historyId());
            assertEquals(LocalDateTime.of(2024, 4, 16, 13, 0), result.expirationAt());
            assertTrue(requestFactory.requestBody().contains("\"labelIds\":[\"INBOX\",\"CATEGORY_PERSONAL\"]"));
            assertTrue(requestFactory.requestBody().contains("\"topicName\":\"projects/test/topics/mail\""));
            assertNull(requestFactory.labelFilterBehaviorValue());
        }

        @Test
        @DisplayName("입력이 유효하지 않으면 watch 실패 예외를 반환한다")
        void watch_입력이유효하지않으면Watch실패예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setWatchUri("https://gmail.googleapis.com/gmail/v1/users/me/watch");
            properties.setTopicName("projects/test/topics/mail");
            properties.setLabelIds(List.of("INBOX", " "));
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(properties, RestClient.builder().build());

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.watch("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-WATCH-FAILED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("응답의 historyId가 비어 있으면 예외를 반환한다")
        void watch_응답의HistoryId가비어있으면예외를반환한다() {
            // given
            GoogleMailProperties properties = createWatchProperties();
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(
                    properties,
                    RestClient.builder().requestFactory(new CapturingClientHttpRequestFactory("""
                            {"historyId":" ","expiration":"1713240000000"}
                            """)).build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.watch("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-WATCH-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("응답의 expiration이 숫자가 아니면 예외를 반환한다")
        void watch_응답의Expiration이숫자가아니면예외를반환한다() {
            // given
            GoogleMailProperties properties = createWatchProperties();
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(
                    properties,
                    RestClient.builder().requestFactory(new CapturingClientHttpRequestFactory("""
                            {"historyId":"history-1","expiration":"invalid"}
                            """)).build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.watch("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-WATCH-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("RestClient 호출이 실패하면 watch 실패 예외를 반환한다")
        void watch_restClient호출이실패하면Watch실패예외를반환한다() {
            // given
            GoogleMailProperties properties = createWatchProperties();
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(
                    properties,
                    RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.watch("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-WATCH-FAILED", exception.getErrorCode().getCode());
        }
    }

    private GoogleMailProperties createWatchProperties() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setWatchUri("https://gmail.googleapis.com/gmail/v1/users/me/watch");
        properties.setTopicName("projects/test/topics/mail");
        properties.setLabelIds(List.of("INBOX"));
        properties.setLabelFilterBehavior("include");
        return properties;
    }

    private static final class CapturingClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        private final String responseBody;
        private String requestBody;

        private CapturingClientHttpRequestFactory(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    requestBody = getBodyAsString();
                    MockClientHttpResponse response = new MockClientHttpResponse(
                            responseBody.getBytes(StandardCharsets.UTF_8),
                            HttpStatus.OK
                    );
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }
            };
        }

        private String requestBody() {
            return requestBody;
        }

        private String labelFilterBehaviorValue() {
            if (requestBody == null || !requestBody.contains("labelFilterBehavior")) {
                return null;
            }
            int keyIndex = requestBody.indexOf("\"labelFilterBehavior\"");
            int start = requestBody.indexOf('"', keyIndex + 22);
            int end = requestBody.indexOf('"', start + 1);
            return requestBody.substring(start + 1, end);
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
