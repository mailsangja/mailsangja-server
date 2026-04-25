package com.mailsangja.core.service.google;

import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
import com.mailsangja.core.dto.mail.GoogleMailReplyContextResult;
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

class GoogleMailMessageReplyQueryServiceTest {

    @Test
    void getReplyContext_답장전송에필요한헤더를추출한다() {
        // given
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
        FixedResponseClientHttpRequestFactory requestFactory = new FixedResponseClientHttpRequestFactory(
                """
                        {
                          "id": "gmail-message-id",
                          "threadId": "gmail-thread-id",
                          "payload": {
                            "headers": [
                              {"name": "Subject", "value": "Re: 프로젝트 논의"},
                              {"name": "Message-ID", "value": "<parent-message@example.com>"},
                              {"name": "References", "value": "<older-message@example.com>"}
                            ]
                          }
                        }
                        """
        );

        GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        // when
        GoogleMailReplyContextResult result = service.getReplyContext("access-token", "gmail-message-id");

        // then
        assertEquals("gmail-thread-id", result.gmailThreadId());
        assertEquals("<parent-message@example.com>", result.parentRfcMessageId());
        assertEquals("<older-message@example.com>", result.referencesHeader());
        assertEquals("Re: 프로젝트 논의", result.subject());
    }

    @Test
    void getMessage_메일식별과답장에필요한헤더를추출한다() {
        // given
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setMessagesUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
        FixedResponseClientHttpRequestFactory requestFactory = new FixedResponseClientHttpRequestFactory(
                """
                        {
                          "id": "gmail-message-id",
                          "threadId": "gmail-thread-id",
                          "historyId": "history-id",
                          "internalDate": "1712822400000",
                          "payload": {
                            "mimeType": "text/plain",
                            "headers": [
                              {"name": "Subject", "value": "프로젝트 논의"},
                              {"name": "From", "value": "Alice <alice@example.com>"},
                              {"name": "Message-ID", "value": "<message-id@example.com>"},
                              {"name": "References", "value": "<root-message@example.com>"},
                              {"name": "In-Reply-To", "value": "<parent-message@example.com>"},
                              {"name": "Reply-To", "value": "\\"Reply Alias\\" <reply@example.com>"}
                            ],
                            "body": {"data": "67O866yA"}
                          }
                        }
                        """
        );

        GoogleMailMessageQueryService service = new GoogleMailMessageQueryService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        // when
        GoogleMailMessageResult result = service.getMessage("access-token", "gmail-message-id");

        // then
        assertEquals("<message-id@example.com>", result.rfcMessageId());
        assertEquals("<root-message@example.com>", result.referencesHeader());
        assertEquals("<parent-message@example.com>", result.inReplyToHeader());
        assertEquals("reply@example.com", result.replyToAddress());
        assertEquals("Reply Alias", result.replyToName());
    }

    private static final class FixedResponseClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        private final String responseBody;

        private FixedResponseClientHttpRequestFactory(String responseBody) {
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
