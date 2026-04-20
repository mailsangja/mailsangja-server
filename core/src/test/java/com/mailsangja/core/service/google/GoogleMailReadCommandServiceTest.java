package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GoogleMailReadCommandService 테스트")
class GoogleMailReadCommandServiceTest {

    @Nested
    @DisplayName("스레드 읽음 상태 변경")
    class ThreadReadState {

        @Test
        @DisplayName("스레드를 읽음 처리하면 UNREAD 라벨을 제거한다")
        void markThreadAsRead_스레드를읽음처리하면Unread라벨을제거한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setThreadModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/threads/{gmailThreadId}/modify");
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

            GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                    properties,
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            service.markThreadAsRead(createMailAccount(), createThread());

            // then
            assertTrue(requestFactory.requestBody().contains("\"removeLabelIds\":[\"UNREAD\"]"));
        }

        @Test
        @DisplayName("스레드를 안읽음 처리하면 UNREAD 라벨을 추가한다")
        void markThreadAsUnread_스레드를안읽음처리하면Unread라벨을추가한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setThreadModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/threads/{gmailThreadId}/modify");
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

            GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                    properties,
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            service.markThreadAsUnread(createMailAccount(), createThread());

            // then
            assertTrue(requestFactory.requestBody().contains("\"addLabelIds\":[\"UNREAD\"]"));
        }

        @Test
        @DisplayName("유효하지 않은 입력이면 안읽음 처리 실패 예외를 반환한다")
        void markThreadAsUnread_유효하지않은입력이면안읽음처리실패예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                    properties,
                    RestClient.builder().build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.markThreadAsUnread(createMailAccount(), createThread())
            );

            // then
            assertEquals(MailAccountErrorCode.GOOGLE_MAIL_UNREAD_MODIFY_FAILED, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("메시지 읽음 상태 변경")
    class MessageReadState {

        @Test
        @DisplayName("메시지를 읽음 처리하면 UNREAD 라벨을 제거한다")
        void markMessageAsRead_메시지를읽음처리하면Unread라벨을제거한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setMessageModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{gmailMessageId}/modify");
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

            GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                    properties,
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            service.markMessageAsRead(createMailAccount(), createMessage());

            // then
            assertTrue(requestFactory.requestBody().contains("\"removeLabelIds\":[\"UNREAD\"]"));
        }

        @Test
        @DisplayName("메시지를 안읽음 처리하면 UNREAD 라벨을 추가한다")
        void markMessageAsUnread_메시지를안읽음처리하면Unread라벨을추가한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setMessageModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{gmailMessageId}/modify");
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

            GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                    properties,
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            service.markMessageAsUnread(createMailAccount(), createMessage());

            // then
            assertTrue(requestFactory.requestBody().contains("\"addLabelIds\":[\"UNREAD\"]"));
        }

        @Test
        @DisplayName("유효하지 않은 입력이면 읽음 처리 실패 예외를 반환한다")
        void markMessageAsRead_유효하지않은입력이면읽음처리실패예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                    properties,
                    RestClient.builder().build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.markMessageAsRead(createMailAccount(), createMessage())
            );

            // then
            assertEquals(MailAccountErrorCode.GOOGLE_MESSAGE_READ_MODIFY_FAILED, exception.getErrorCode());
        }

        @Test
        @DisplayName("유효하지 않은 입력이면 안읽음 처리 실패 예외를 반환한다")
        void markMessageAsUnread_유효하지않은입력이면안읽음처리실패예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                    properties,
                    RestClient.builder().build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.markMessageAsUnread(createMailAccount(), createMessage())
            );

            // then
            assertEquals(MailAccountErrorCode.GOOGLE_MESSAGE_UNREAD_MODIFY_FAILED, exception.getErrorCode());
        }

        @Test
        @DisplayName("호출 실패 시 동일한 에러코드로 예외를 반환한다")
        void markMessageAsUnread_호출실패시동일한에러코드로예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setMessageModifyUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{gmailMessageId}/modify");

            GoogleMailReadCommandService service = new GoogleMailReadCommandService(
                    properties,
                    RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
            );

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> service.markMessageAsUnread(createMailAccount(), createMessage())
            );

            // then
            assertEquals(MailAccountErrorCode.GOOGLE_MESSAGE_UNREAD_MODIFY_FAILED, exception.getErrorCode());
        }
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
