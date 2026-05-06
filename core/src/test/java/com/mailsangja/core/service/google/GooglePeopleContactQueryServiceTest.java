package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.config.properties.GooglePeopleProperties;
import com.mailsangja.core.dto.contact.GoogleContactResult;
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
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GooglePeopleContactQueryServiceTest {

    @Test
    void getContacts_페이지네이션을따라가며이메일을정규화하고중복을제거한다() {
        GooglePeopleProperties properties = createProperties();
        CapturingPagedRequestFactory requestFactory = new CapturingPagedRequestFactory(
                """
                        {
                          "connections": [
                            {
                              "names": [{"displayName": " Alice "}],
                              "emailAddresses": [{"value": " Alice@Example.com "}]
                            },
                            {
                              "names": [{"displayName": "Duplicate"}],
                              "emailAddresses": [{"value": "alice@example.com"}]
                            },
                            {
                              "names": [{"givenName": "Bob", "familyName": "Kim"}],
                              "emailAddresses": [{"value": "bob@example.com"}]
                            },
                            {
                              "names": [{"displayName": "No Email"}],
                              "emailAddresses": []
                            }
                          ],
                          "nextPageToken": "next-token"
                        }
                        """,
                """
                        {
                          "connections": [
                            {
                              "names": [],
                              "emailAddresses": [{"value": "carol@example.com"}]
                            }
                          ]
                        }
                        """
        );
        GooglePeopleContactQueryService service = new GooglePeopleContactQueryService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        List<GoogleContactResult> results = service.getContacts("access-token");

        assertEquals(3, results.size());
        assertEquals(new GoogleContactResult("Alice", "alice@example.com"), results.get(0));
        assertEquals(new GoogleContactResult("Bob Kim", "bob@example.com"), results.get(1));
        assertEquals(new GoogleContactResult("carol@example.com", "carol@example.com"), results.get(2));

        assertEquals(2, requestFactory.requestUris().size());
        assertTrue(requestFactory.requestUris().get(0).contains("personFields=names,emailAddresses"));
        assertTrue(requestFactory.requestUris().get(0).contains("pageSize=1000"));
        assertTrue(requestFactory.requestUris().get(1).contains("pageToken=next-token"));
        assertEquals(List.of("Bearer access-token", "Bearer access-token"), requestFactory.authorizationHeaders());
    }

    @Test
    void getContacts_응답호출이실패하면MailAccountException을던진다() {
        GooglePeopleProperties properties = createProperties();
        GooglePeopleContactQueryService service = new GooglePeopleContactQueryService(
                properties,
                RestClient.builder().requestFactory(new FailingRequestFactory()).build()
        );

        ContactException exception = assertThrows(
                ContactException.class,
                () -> service.getContacts("access-token")
        );

        assertEquals(ContactErrorCode.GOOGLE_CONTACTS_FETCH_FAILED, exception.getErrorCode());
    }

    @Test
    void getContacts_accessToken이공백이면API를호출하지않고실패한다() {
        CapturingPagedRequestFactory requestFactory = new CapturingPagedRequestFactory("{}");
        GooglePeopleContactQueryService service = new GooglePeopleContactQueryService(
                createProperties(),
                RestClient.builder().requestFactory(requestFactory).build()
        );

        ContactException exception = assertThrows(
                ContactException.class,
                () -> service.getContacts(" ")
        );

        assertEquals(ContactErrorCode.GOOGLE_CONTACTS_FETCH_FAILED, exception.getErrorCode());
        assertEquals(0, requestFactory.requestUris().size());
    }

    @Test
    void getContacts_connectionsUri가공백이면API를호출하지않고실패한다() {
        GooglePeopleProperties properties = createProperties();
        properties.setConnectionsUri(" ");
        CapturingPagedRequestFactory requestFactory = new CapturingPagedRequestFactory("{}");
        GooglePeopleContactQueryService service = new GooglePeopleContactQueryService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        ContactException exception = assertThrows(
                ContactException.class,
                () -> service.getContacts("access-token")
        );

        assertEquals(ContactErrorCode.GOOGLE_CONTACTS_FETCH_FAILED, exception.getErrorCode());
        assertEquals(0, requestFactory.requestUris().size());
    }

    @Test
    void getContacts_pageSize가0이하면API를호출하지않고실패한다() {
        GooglePeopleProperties properties = createProperties();
        properties.setPageSize(0);
        CapturingPagedRequestFactory requestFactory = new CapturingPagedRequestFactory("{}");
        GooglePeopleContactQueryService service = new GooglePeopleContactQueryService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        ContactException exception = assertThrows(
                ContactException.class,
                () -> service.getContacts("access-token")
        );

        assertEquals(ContactErrorCode.GOOGLE_CONTACTS_FETCH_FAILED, exception.getErrorCode());
        assertEquals(0, requestFactory.requestUris().size());
    }

    @Test
    void getContacts_응답이비어있으면결과오류로실패한다() {
        GooglePeopleContactQueryService service = new GooglePeopleContactQueryService(
                createProperties(),
                RestClient.builder().requestFactory(new EmptyBodyRequestFactory()).build()
        );

        ContactException exception = assertThrows(
                ContactException.class,
                () -> service.getContacts("access-token")
        );

        assertEquals(ContactErrorCode.GOOGLE_CONTACTS_RESULT_INVALID, exception.getErrorCode());
    }

    @Test
    void getContacts_한연락처에이메일이여러개면각이메일을별도결과로반환한다() {
        GooglePeopleContactQueryService service = new GooglePeopleContactQueryService(
                createProperties(),
                RestClient.builder().requestFactory(new CapturingPagedRequestFactory("""
                        {
                          "connections": [
                            {
                              "names": [{"displayName": "Multi Email"}],
                              "emailAddresses": [
                                {"value": "first@example.com"},
                                {"value": "second@example.com"}
                              ]
                            }
                          ]
                        }
                        """)).build()
        );

        List<GoogleContactResult> results = service.getContacts("access-token");

        assertEquals(List.of(
                new GoogleContactResult("Multi Email", "first@example.com"),
                new GoogleContactResult("Multi Email", "second@example.com")
        ), results);
    }

    private GooglePeopleProperties createProperties() {
        GooglePeopleProperties properties = new GooglePeopleProperties();
        properties.setConnectionsUri("https://people.googleapis.com/v1/people/me/connections");
        properties.setPageSize(1000);
        return properties;
    }

    private static final class CapturingPagedRequestFactory extends SimpleClientHttpRequestFactory {
        private final List<String> responses;
        private final List<String> requestUris = new ArrayList<>();
        private final List<String> authorizationHeaders = new ArrayList<>();
        private int requestIndex;

        private CapturingPagedRequestFactory(String... responses) {
            this.responses = List.of(responses);
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    requestUris.add(uri.toString());
                    authorizationHeaders.add(getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

                    MockClientHttpResponse response = new MockClientHttpResponse(
                            responses.get(requestIndex++).getBytes(StandardCharsets.UTF_8),
                            HttpStatus.OK
                    );
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }
            };
        }

        private List<String> requestUris() {
            return requestUris;
        }

        private List<String> authorizationHeaders() {
            return authorizationHeaders;
        }
    }

    private static final class FailingRequestFactory extends SimpleClientHttpRequestFactory {
        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    return new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR);
                }
            };
        }
    }

    private static final class EmptyBodyRequestFactory extends SimpleClientHttpRequestFactory {
        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }
            };
        }
    }
}
