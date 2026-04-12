package com.mailsangja.worker.service.google;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.gmail.GoogleMailThreadResponse;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleMailMessageQueryServiceTest {

    @Test
    void getThreads_normalizesMailAddressesFromHeaders() {
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

        List<InitialMailSyncThreadResult> results = service.getThreads("token", List.of("thread-1"));

        assertEquals(1, results.size());
        assertEquals(1, results.getFirst().messages().size());
        assertEquals("alice@example.com", results.getFirst().messages().getFirst().fromAddress());
        assertEquals(List.of("bob@example.com", "carol@example.com"), results.getFirst().messages().getFirst().toAddresses());
        assertEquals(List.of("dave@example.com"), results.getFirst().messages().getFirst().ccAddresses());
        assertEquals(Direction.INBOUND, results.getFirst().messages().getFirst().direction());
        assertEquals("hello", results.getFirst().messages().getFirst().bodyText());
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
                    return new MockClientHttpResponse(
                            responseBody.getBytes(StandardCharsets.UTF_8),
                            HttpStatus.OK
                    );
                }
            };
        }
    }
}
