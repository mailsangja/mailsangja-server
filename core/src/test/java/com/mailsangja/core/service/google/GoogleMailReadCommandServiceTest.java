package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Thread;
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
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleMailReadCommandServiceTest {

    @Test
    void markThreadAsRead_UNREAD라벨을제거한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setThreadModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/threads/{gmailThreadId}/modify");
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

        GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        service.markThreadAsRead(createMailAccount(), createThread());

        assertTrue(requestFactory.requestBody().contains("\"removeLabelIds\":[\"UNREAD\"]"));
    }

    @Test
    void markThreadAsUnread_UNREAD라벨을추가한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setThreadModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/threads/{gmailThreadId}/modify");
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

        GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        service.markThreadAsUnread(createMailAccount(), createThread());

        assertTrue(requestFactory.requestBody().contains("\"addLabelIds\":[\"UNREAD\"]"));
    }

    @Test
    void markMessageAsRead_UNREAD라벨을제거한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setMessageModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{gmailMessageId}/modify");
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

        GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        service.markMessageAsRead(createMailAccount(), createMessage());

        assertTrue(requestFactory.requestBody().contains("\"removeLabelIds\":[\"UNREAD\"]"));
    }

    @Test
    void markMessageAsUnread_UNREAD라벨을추가한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setMessageModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{gmailMessageId}/modify");
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

        GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        service.markMessageAsUnread(createMailAccount(), createMessage());

        assertTrue(requestFactory.requestBody().contains("\"addLabelIds\":[\"UNREAD\"]"));
    }

    @Test
    void markThreadAsUnread_유효하지않은입력이면예외가발생한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                properties,
                RestClient.builder().build()
        );

        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.markThreadAsUnread(createMailAccount(), createThread())
        );

        assertEquals(MailAccountErrorCode.GOOGLE_MAIL_UNREAD_MODIFY_FAILED, exception.getErrorCode());
    }

    @Test
    void markMessageAsRead_유효하지않은입력이면예외가발생한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                properties,
                RestClient.builder().build()
        );

        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.markMessageAsRead(createMailAccount(), createMessage())
        );

        assertEquals(MailAccountErrorCode.GOOGLE_MESSAGE_READ_MODIFY_FAILED, exception.getErrorCode());
    }

    @Test
    void markMessageAsUnread_유효하지않은입력이면예외가발생한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                properties,
                RestClient.builder().build()
        );

        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.markMessageAsUnread(createMailAccount(), createMessage())
        );

        assertEquals(MailAccountErrorCode.GOOGLE_MESSAGE_UNREAD_MODIFY_FAILED, exception.getErrorCode());
    }

    @Test
    void markMessageAsUnread_호출실패시동일한에러코드로예외를반환한다() {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setMessageModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{gmailMessageId}/modify");

        GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                properties,
                RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
        );

        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> service.markMessageAsUnread(createMailAccount(), createMessage())
        );

        assertEquals(MailAccountErrorCode.GOOGLE_MESSAGE_UNREAD_MODIFY_FAILED, exception.getErrorCode());
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("access-token")
                .build();
    }

    private Thread createThread() {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(createMailAccount())
                .gmailThreadId("gmail-thread-id")
                .build();
    }

    private Message createMessage() {
        Thread thread = createThread();
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message-id")
                .direction(com.mailsangja.db.entity.mail.Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
    }

    private static final class CapturingClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        private String requestBody;

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    requestBody = getBodyAsString();
                    MockClientHttpResponse response = new MockClientHttpResponse(
                            "{}".getBytes(StandardCharsets.UTF_8),
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
