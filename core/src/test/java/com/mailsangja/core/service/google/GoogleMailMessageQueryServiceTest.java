package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
import com.mailsangja.db.entity.mail.AttachmentDisposition;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleMailMessageQueryServiceTest {

    @Test
    void getMessage_internalDate가없으면유효하지않은응답예외가발생한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");

        RestClient restClient = RestClient.builder()
                .requestFactory(new StubClientHttpRequestFactory("""
                        {
                          "id": "message-1",
                          "threadId": "thread-1",
                          "payload": {
                            "headers": [
                              {"name": "From", "value": "Alice <alice@example.com>"}
                            ]
                          }
                        }
                        """))
                .build();
        GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(properties, restClient);

        assertThrows(MailSendException.class, () -> service.getMessage("token", "message-1"));
    }

    @Test
    void getMessage_본문이미지첨부의contentId와disposition을보존한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");

        RestClient restClient = RestClient.builder()
                .requestFactory(new StubClientHttpRequestFactory("""
                        {
                          "id": "message-1",
                          "threadId": "thread-1",
                          "snippet": "snippet",
                          "historyId": "history-1",
                          "internalDate": "1712822400000",
                          "payload": {
                            "mimeType": "multipart/related",
                            "headers": [
                              {"name": "Subject", "value": "subject"},
                              {"name": "From", "value": "Alice <alice@example.com>"},
                              {"name": "To", "value": "Bob <bob@example.com>"},
                              {"name": "Message-ID", "value": "<message-id@example.com>"}
                            ],
                            "parts": [
                              {
                                "mimeType": "text/html",
                                "body": {"data": "PHA-Ym9keTxpbWcgc3JjPVwiY2lkOmlubGluZS0xXCI-PC9wPg"}
                              },
                              {
                                "mimeType": "image/png",
                                "filename": "image.png",
                                "headers": [
                                  {"name": "Content-ID", "value": "<inline-1>"},
                                  {"name": "Content-Disposition", "value": "inline; filename=image.png"}
                                ],
                                "body": {"attachmentId": "gmail-attachment-id", "size": 5}
                              }
                            ]
                          }
                        }
                        """))
                .build();

        GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(properties, restClient);

        GoogleMailMessageResult result = service.getMessage("token", "message-1");

        assertEquals(1, result.attachments().size());
        assertEquals("gmail-attachment-id", result.attachments().getFirst().gmailAttachmentId());
        assertEquals("inline-1", result.attachments().getFirst().contentId());
        assertEquals(AttachmentDisposition.INLINE, result.attachments().getFirst().disposition());
    }

    @Test
    void getMessage_contentId가있어도첨부파일Disposition이면Attachment로분류한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");

        RestClient restClient = RestClient.builder()
                .requestFactory(new StubClientHttpRequestFactory("""
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
                              {"name": "To", "value": "Bob <bob@example.com>"},
                              {"name": "Message-ID", "value": "<message-id@example.com>"}
                            ],
                            "parts": [
                              {
                                "mimeType": "text/plain",
                                "body": {"data": "aGVsbG8"}
                              },
                              {
                                "mimeType": "application/haansofthwp",
                                "filename": "document.hwp",
                                "headers": [
                                  {"name": "Content-ID", "value": "<f_mpmdzs8f0>"},
                                  {"name": "Content-Disposition", "value": "attachment; filename=document.hwp"}
                                ],
                                "body": {"attachmentId": "gmail-attachment-id", "size": 100}
                              }
                            ]
                          }
                        }
                        """))
                .build();

        GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(properties, restClient);

        GoogleMailMessageResult result = service.getMessage("token", "message-1");

        assertEquals(1, result.attachments().size());
        assertEquals("f_mpmdzs8f0", result.attachments().getFirst().contentId());
        assertEquals(AttachmentDisposition.ATTACHMENT, result.attachments().getFirst().disposition());
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
