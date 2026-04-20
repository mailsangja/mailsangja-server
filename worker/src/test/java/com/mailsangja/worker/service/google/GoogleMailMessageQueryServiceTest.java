package com.mailsangja.worker.service.google;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.gmail.message.GoogleMailMessageListResult;
import com.mailsangja.worker.dto.gmail.message.GoogleMailThreadResponse;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
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
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GoogleMailMessageQueryService 테스트")
class GoogleMailMessageQueryServiceTest {

    @Nested
    @DisplayName("getLatestMessages")
    class GetLatestMessages {

        @Test
        @DisplayName("resultSizeEstimate가 없으면 fetched count를 사용한다")
        void getLatestMessages_resultSizeEstimate가없으면FetchedCount를사용한다() {
            GoogleMailInitialSyncProperties properties = createProperties();
            RestClient restClient = RestClient.builder()
                    .requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "messages": [
                                {"id": "message-1", "threadId": "thread-1"},
                                {"id": "message-2", "threadId": "thread-2"}
                              ]
                            }
                            """))
                    .build();
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(properties, restClient);

            GoogleMailMessageListResult result = service.getLatestMessages("token");

            assertEquals(2, result.resultSizeEstimate());
            assertEquals(2, result.messages().size());
        }

        @Test
        @DisplayName("message id가 없는 응답이면 예외를 반환한다")
        void getLatestMessages_messageId가없는응답이면예외를반환한다() {
            GoogleMailInitialSyncProperties properties = createProperties();
            RestClient restClient = RestClient.builder()
                    .requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "messages": [
                                {"threadId": "thread-1"}
                              ]
                            }
                            """))
                    .build();
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(properties, restClient);

            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.getLatestMessages("token")
            );

            assertEquals("MS-MAIL-GMAIL-MESSAGES-RESULT-INVALID", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("getThreads")
    class GetThreads {

        @Test
        @DisplayName("메일 헤더를 정규화하고 plain body를 추출한다")
        void getThreads_메일헤더를정규화하고PlainBody를추출한다() {
            // given
            GoogleMailInitialSyncProperties properties = new GoogleMailInitialSyncProperties();
            properties.setThreadsUri("https://gmail.googleapis.com/gmail/v1/users/me/threads");

            RestClient restClient = RestClient.builder()
                    .requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "id": "thread-1",
                              "historyId": "history-1",
                              "messages": [
                                {
                                  "id": "message-1",
                                  "threadId": "thread-1",
                                  "labelIds": ["INBOX"],
                                  "snippet": "snippet",
                                  "historyId": "history-1",
                                  "internalDate": "1712822400000",
                                  "payload": {
                                    "mimeType": "multipart/alternative",
                                    "headers": [
                                      {"name": "Subject", "value": "subject"},
                                      {"name": "From", "value": "Alice Kim <alice@example.com>"},
                                      {"name": "To", "value": "Bob <bob@example.com>, carol@example.com"},
                                      {"name": "Cc", "value": "\\"Dave, Jr.\\" <dave@example.com>"}
                                    ],
                                    "parts": [
                                      {
                                        "mimeType": "text/plain",
                                        "body": {"data": "aGVsbG8"}
                                      }
                                    ]
                                  }
                                }
                              ]
                            }
                            """))
                    .build();

            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(properties, restClient);

            // when
            List<InitialMailSyncThreadResult> results = service.getThreads("token", List.of("thread-1"));

            // then
            assertEquals(1, results.size());
            assertEquals(1, results.getFirst().messages().size());
            assertEquals("alice@example.com", results.getFirst().messages().getFirst().fromAddress());
            assertEquals("Alice Kim", results.getFirst().messages().getFirst().fromName());
            assertEquals(List.of("bob@example.com", "carol@example.com"), results.getFirst().messages().getFirst().toAddresses());
            assertEquals(Arrays.asList("Bob", "carol@example.com"), results.getFirst().messages().getFirst().toNames());
            assertEquals(List.of("dave@example.com"), results.getFirst().messages().getFirst().ccAddresses());
            assertEquals(List.of("Dave, Jr."), results.getFirst().messages().getFirst().ccNames());
            assertEquals(Direction.INBOUND, results.getFirst().messages().getFirst().direction());
            assertEquals("hello", results.getFirst().messages().getFirst().bodyText());
        }

        @Test
        @DisplayName("html body와 첨부파일을 포함하면 outbound 메시지로 변환한다")
        void getThreads_htmlBody와첨부파일을포함하면Outbound메시지로변환한다() {
            GoogleMailInitialSyncProperties properties = createProperties();
            RestClient restClient = RestClient.builder()
                    .requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "id": "thread-1",
                              "historyId": "history-9",
                              "messages": [
                                {
                                  "id": "message-1",
                                  "threadId": "thread-1",
                                  "labelIds": ["SENT", "INBOX"],
                                  "snippet": "snippet",
                                  "internalDate": "1712822400000",
                                  "payload": {
                                    "mimeType": "multipart/mixed",
                                    "headers": [
                                      {"name": "From", "value": "sender@example.com"},
                                      {"name": "To", "value": "Receiver <receiver@example.com>"}
                                    ],
                                    "parts": [
                                      {
                                        "mimeType": "multipart/alternative",
                                        "parts": [
                                          {
                                            "mimeType": "text/html",
                                            "body": {"data": "PGI-aHRtbDwvYj4"}
                                          }
                                        ]
                                      },
                                      {
                                        "filename": "file.txt",
                                        "mimeType": "text/plain",
                                        "body": {"attachmentId": "att-1", "size": 12}
                                      }
                                    ]
                                  }
                                }
                              ]
                            }
                            """))
                    .build();
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(properties, restClient);

            List<InitialMailSyncThreadResult> results = service.getThreads("token", List.of("thread-1"));

            assertEquals(Direction.OUTBOUND, results.getFirst().messages().getFirst().direction());
            assertEquals("<b>html</b>", results.getFirst().messages().getFirst().bodyHtml());
            assertEquals(1, results.getFirst().messages().getFirst().attachments().size());
            assertEquals("att-1", results.getFirst().messages().getFirst().attachments().getFirst().gmailAttachmentId());
        }

        @Test
        @DisplayName("From 헤더가 잘못되면 예외를 반환한다")
        void getThreads_from헤더가잘못되면예외를반환한다() {
            GoogleMailInitialSyncProperties properties = createProperties();
            RestClient restClient = RestClient.builder()
                    .requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "id": "thread-1",
                              "historyId": "history-1",
                              "messages": [
                                {
                                  "id": "message-1",
                                  "threadId": "thread-1",
                                  "payload": {
                                    "headers": [
                                      {"name": "From", "value": "bad<address"}
                                    ]
                                  }
                                }
                              ]
                            }
                            """))
                    .build();
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(properties, restClient);

            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.getThreads("token", List.of("thread-1"))
            );

            assertEquals("MS-MAIL-GMAIL-MESSAGES-RESULT-INVALID", exception.getErrorCode().getCode());
        }
    }

    private GoogleMailInitialSyncProperties createProperties() {
        GoogleMailInitialSyncProperties properties = new GoogleMailInitialSyncProperties();
        properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
        properties.setThreadsUri("https://gmail.googleapis.com/gmail/v1/users/me/threads");
        properties.setMaxResults(10);
        properties.setThreadBatchSize(10);
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
                    MockClientHttpResponse response = new MockClientHttpResponse(
                            responseBody.getBytes(StandardCharsets.UTF_8),
                            HttpStatus.OK
                    );
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }
            };
        }
    }
}
