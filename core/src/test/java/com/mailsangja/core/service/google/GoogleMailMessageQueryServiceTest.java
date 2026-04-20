package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailAttachmentResult;
import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GoogleMailMessageQueryService 테스트")
class GoogleMailMessageQueryServiceTest {

    @Nested
    @DisplayName("메시지 조회")
    class GetMessage {

        @Test
        @DisplayName("유효한 Gmail 메시지 응답이면 본문과 첨부 정보를 파싱한다")
        void getMessage_유효한Gmail메시지응답이면본문과첨부정보를파싱한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(
                    properties,
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "id": "message-1",
                              "threadId": "thread-1",
                              "snippet": "snippet",
                              "historyId": "history-1",
                              "internalDate": "1712822400000",
                              "payload": {
                                "mimeType": "multipart/mixed",
                                "headers": [
                                  {"name": "Subject", "value": "subject"},
                                  {"name": "From", "value": "Alice <alice@example.com>"},
                                  {"name": "To", "value": "Bob <bob@example.com>, carol@example.com"},
                                  {"name": "Cc", "value": "\\"Dave, Jr.\\" <dave@example.com>"}
                                ],
                                "parts": [
                                  {
                                    "mimeType": "multipart/alternative",
                                    "parts": [
                                      {
                                        "mimeType": "text/plain",
                                        "body": {"data": "aGVsbG8"}
                                      },
                                      {
                                        "mimeType": "text/html",
                                        "body": {"data": "PGRpdj5oZWxsbzwvZGl2Pg"}
                                      }
                                    ]
                                  },
                                  {
                                    "mimeType": "application/pdf",
                                    "filename": "guide.pdf",
                                    "body": {
                                      "size": 12,
                                      "attachmentId": "attachment-1"
                                    }
                                  }
                                ]
                              }
                            }
                            """)).build()
            );

            // when
            GoogleMailMessageResult result = service.getMessage("access-token", "message-1");

            // then
            assertEquals("message-1", result.gmailMessageId());
            assertEquals("thread-1", result.gmailThreadId());
            assertEquals("alice@example.com", result.fromAddress());
            assertEquals("Alice", result.fromName());
            assertEquals(List.of("bob@example.com", "carol@example.com"), result.toAddresses());
            assertEquals(List.of("Bob", "carol@example.com"), result.toNames());
            assertEquals(List.of("dave@example.com"), result.ccAddresses());
            assertEquals(List.of("Dave, Jr."), result.ccNames());
            assertEquals(LocalDateTime.of(2024, 4, 11, 17, 0), result.sentAt());
            assertEquals("hello", result.bodyText());
            assertEquals("<div>hello</div>", result.bodyHtml());
            assertEquals(1, result.attachments().size());
            GoogleMailAttachmentResult attachment = result.attachments().getFirst();
            assertEquals("guide.pdf", attachment.filename());
            assertEquals("application/pdf", attachment.mimeType());
            assertEquals(12, attachment.size());
            assertEquals("attachment-1", attachment.gmailAttachmentId());
        }

        @Test
        @DisplayName("From 헤더가 비어 있으면 결과가 유효하지 않다고 판단한다")
        void getMessage_from헤더가비어있으면결과가유효하지않다고판단한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(
                    properties,
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "id": "message-1",
                              "threadId": "thread-1",
                              "payload": {
                                "mimeType": "text/plain",
                                "headers": [
                                  {"name": "Subject", "value": "subject"}
                                ],
                                "body": {"data": "aGVsbG8"}
                              }
                            }
                            """)).build()
            );

            // when
            MailSendException exception = assertThrows(
                    MailSendException.class,
                    () -> service.getMessage("access-token", "message-1")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-MESSAGE-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("Cc 헤더가 잘못되어도 선택 항목이므로 빈 목록으로 처리한다")
        void getMessage_cc헤더가잘못되어도선택항목이므로빈목록으로처리한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(
                    properties,
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "id": "message-1",
                              "threadId": "thread-1",
                              "payload": {
                                "mimeType": "text/plain",
                                "headers": [
                                  {"name": "From", "value": "Alice <alice@example.com>"},
                                  {"name": "Cc", "value": "Alice <alice@example.com"}
                                ],
                                "body": {"data": "aGVsbG8"}
                              }
                            }
                            """)).build()
            );

            // when
            GoogleMailMessageResult result = service.getMessage("access-token", "message-1");

            // then
            assertEquals(List.of(), result.ccAddresses());
            assertEquals(List.of(), result.ccNames());
            assertNull(result.sentAt());
        }

        @Test
        @DisplayName("응답의 internalDate가 숫자가 아니면 예외를 반환한다")
        void getMessage_internalDate가숫자가아니면예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(
                    properties,
                    RestClient.builder().requestFactory(new StubClientHttpRequestFactory("""
                            {
                              "id": "message-1",
                              "threadId": "thread-1",
                              "internalDate": "not-a-number",
                              "payload": {
                                "mimeType": "text/plain",
                                "headers": [
                                  {"name": "From", "value": "Alice <alice@example.com>"}
                                ],
                                "body": {"data": "aGVsbG8"}
                              }
                            }
                            """)).build()
            );

            // when
            MailSendException exception = assertThrows(
                    MailSendException.class,
                    () -> service.getMessage("access-token", "message-1")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-MESSAGE-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("RestClient 예외가 발생하면 메시지 조회 실패 예외를 반환한다")
        void getMessage_restClient예외가발생하면메시지조회실패예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(
                    properties,
                    RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
            );

            // when
            MailSendException exception = assertThrows(
                    MailSendException.class,
                    () -> service.getMessage("access-token", "message-1")
            );

            // then
            assertEquals("MS-MAIL-GOOGLE-MAIL-MESSAGE-FETCH-FAILED", exception.getErrorCode().getCode());
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
