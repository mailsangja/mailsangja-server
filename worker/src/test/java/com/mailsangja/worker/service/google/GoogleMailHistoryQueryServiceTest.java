package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailHistoryProperties;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryListResult;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GoogleMailHistoryQueryService 테스트")
class GoogleMailHistoryQueryServiceTest {

    @Nested
    @DisplayName("getHistory")
    class GetHistory {

        @Test
        @DisplayName("유효한 응답이면 history item들을 결과 객체로 매핑한다")
        void getHistory_유효한응답이면HistoryItem들을결과객체로매핑한다() {
            // given
            GoogleMailHistoryProperties properties = new GoogleMailHistoryProperties();
            properties.setHistoryUri("https://gmail.googleapis.com/gmail/v1/users/me/history");
            GoogleMailHistoryQueryService service = new GoogleMailHistoryQueryService(
                    properties,
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "historyId": "history-2",
                              "history": [
                                {
                                  "id": "history-1",
                                  "labelsAdded": [
                                    {
                                      "message": {"id": "message-1", "threadId": "thread-1"},
                                      "labelIds": ["INBOX"]
                                    }
                                  ],
                                  "messagesDeleted": [
                                    null
                                  ],
                                  "messagesAdded": [
                                    {
                                      "message": {"id": "message-2", "threadId": "thread-2"}
                                    }
                                  ]
                                }
                              ]
                            }
                            """)).build()
            );

            // when
            GoogleMailHistoryListResult result = service.getHistory("access-token", "history-1");

            // then
            assertEquals("history-2", result.historyId());
            assertEquals(1, result.histories().size());
            assertEquals("history-1", result.histories().getFirst().historyId());
            assertEquals("message-1", result.histories().getFirst().labelsAdded().getFirst().gmailMessageId());
            assertEquals(List.of("INBOX"), result.histories().getFirst().labelsAdded().getFirst().labelIds());
            assertEquals("message-2", result.histories().getFirst().messagesAdded().getFirst().gmailMessageId());
            assertEquals(1, result.histories().getFirst().messagesDeleted().size());
        }

        @Test
        @DisplayName("historyId가 없으면 결과가 유효하지 않다고 판단한다")
        void getHistory_historyId가없으면결과가유효하지않다고판단한다() {
            // given
            GoogleMailHistoryProperties properties = new GoogleMailHistoryProperties();
            properties.setHistoryUri("https://gmail.googleapis.com/gmail/v1/users/me/history");
            GoogleMailHistoryQueryService service = new GoogleMailHistoryQueryService(
                    properties,
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "history": []
                            }
                            """)).build()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.getHistory("access-token", "history-1")
            );

            // then
            assertEquals("MS-MAIL-GMAIL-HISTORY-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("RestClient 예외가 발생하면 조회 실패 예외를 반환한다")
        void getHistory_restClient예외가발생하면조회실패예외를반환한다() {
            // given
            GoogleMailHistoryProperties properties = new GoogleMailHistoryProperties();
            properties.setHistoryUri("https://gmail.googleapis.com/gmail/v1/users/me/history");
            GoogleMailHistoryQueryService service = new GoogleMailHistoryQueryService(
                    properties,
                    RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.getHistory("access-token", "history-1")
            );

            // then
            assertEquals("MS-MAIL-GMAIL-HISTORY-FETCH-FAILED", exception.getErrorCode().getCode());
        }
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
