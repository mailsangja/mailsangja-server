package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailWatchProperties;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GoogleMailWatchQueryService 테스트")
class GoogleMailWatchQueryServiceTest {

    @Nested
    @DisplayName("watch")
    class Watch {

        @Test
        @DisplayName("유효한 응답이면 historyId와 expiration을 결과로 변환한다")
        void watch_유효한응답이면HistoryId와Expiration을결과로변환한다() {
            // given
            GoogleMailWatchProperties properties = createProperties();
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(
                    properties,
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "historyId": "history-55",
                              "expiration": "1776646800000"
                            }
                            """)).build()
            );

            // when
            GoogleMailWatchResult result = service.watch("access-token");

            // then
            assertEquals("history-55", result.historyId());
            assertEquals(LocalDateTime.of(2026, 4, 20, 10, 0), result.expirationAt());
        }

        @Test
        @DisplayName("labelIds에 공백 값이 포함되면 예외를 반환한다")
        void watch_labelIds에공백값이포함되면예외를반환한다() {
            // given
            GoogleMailWatchProperties properties = createProperties();
            properties.setLabelIds(List.of("INBOX", " "));
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(
                    properties,
                    RestClient.builder().build()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.watch("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-WATCH-FAILED", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("expiration이 숫자가 아니면 결과가 유효하지 않다고 판단한다")
        void watch_expiration이숫자가아니면결과가유효하지않다고판단한다() {
            // given
            GoogleMailWatchProperties properties = createProperties();
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(
                    properties,
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "historyId": "history-55",
                              "expiration": "not-a-number"
                            }
                            """)).build()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.watch("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-WATCH-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("RestClient 예외가 발생하면 watch 실패 예외를 반환한다")
        void watch_restClient예외가발생하면Watch실패예외를반환한다() {
            // given
            GoogleMailWatchProperties properties = createProperties();
            GoogleMailWatchQueryService service = new GoogleMailWatchQueryService(
                    properties,
                    RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.watch("access-token")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-WATCH-FAILED", exception.getErrorCode().getCode());
        }
    }

    private GoogleMailWatchProperties createProperties() {
        GoogleMailWatchProperties properties = new GoogleMailWatchProperties();
        properties.setTopicName("projects/test/topics/mailbox");
        properties.setWatchUri("https://gmail.googleapis.com/gmail/v1/users/me/watch");
        properties.setLabelIds(List.of("INBOX"));
        properties.setLabelFilterBehavior("include");
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
