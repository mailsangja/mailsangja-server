package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GoogleMailAttachmentQueryService 테스트")
class GoogleMailAttachmentQueryServiceTest {

    @Nested
    @DisplayName("첨부파일 다운로드")
    class Download {

        @Test
        @DisplayName("정상 응답이면 첨부파일 데이터를 디코딩한다")
        void download_정상응답이면첨부파일데이터를디코딩한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setAttachmentsUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory("""
                    {"attachmentId":"attachment-1","size":5,"data":"aGVsbG8="}
                    """);
            GoogleMailAttachmentQueryService service = new GoogleMailAttachmentQueryService(
                    properties,
                    RestClient.builder().requestFactory(requestFactory).build()
            );

            // when
            byte[] result = service.download(createMailAccount(), createMessage(), createAttachment());

            // then
            assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), result);
            assertEquals(
                    "https://gmail.googleapis.com/gmail/v1/users/me/messages/gmail-message-id/attachments/attachment-1",
                    requestFactory.requestUri()
            );
        }

        @Test
        @DisplayName("응답 데이터가 비어 있고 크기가 0이면 빈 배열을 반환한다")
        void download_응답데이터가비어있고크기가0이면빈배열을반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setAttachmentsUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailAttachmentQueryService service = new GoogleMailAttachmentQueryService(
                    properties,
                    RestClient.builder().requestFactory(new CapturingClientHttpRequestFactory("""
                            {"attachmentId":"attachment-1","size":0,"data":" "}
                            """)).build()
            );

            // when
            byte[] result = service.download(createMailAccount(), createMessage(), createAttachment());

            // then
            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("입력이 유효하지 않으면 첨부 소스 예외를 반환한다")
        void download_입력이유효하지않으면첨부소스예외를반환한다() {
            // given
            GoogleMailAttachmentQueryService service = new GoogleMailAttachmentQueryService(
                    new GoogleMailProperties(),
                    RestClient.builder().build()
            );

            // when
            InboxException exception = assertThrows(
                    InboxException.class,
                    () -> service.download(createMailAccount(), createMessage(), createAttachment())
            );

            // then
            assertEquals("MS-INBOX-ATTACHMENT-SOURCE-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("응답이 없으면 첨부 소스 예외를 반환한다")
        void download_응답이없으면첨부소스예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setAttachmentsUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailAttachmentQueryService service = new GoogleMailAttachmentQueryService(
                    properties,
                    RestClient.builder().requestFactory(new CapturingClientHttpRequestFactory("")).build()
            );

            // when
            InboxException exception = assertThrows(
                    InboxException.class,
                    () -> service.download(createMailAccount(), createMessage(), createAttachment())
            );

            // then
            assertEquals("MS-INBOX-ATTACHMENT-SOURCE-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("응답 데이터가 Base64 형식이 아니면 첨부 소스 예외를 반환한다")
        void download_응답데이터가Base64형식이아니면첨부소스예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setAttachmentsUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailAttachmentQueryService service = new GoogleMailAttachmentQueryService(
                    properties,
                    RestClient.builder().requestFactory(new CapturingClientHttpRequestFactory("""
                            {"attachmentId":"attachment-1","size":5,"data":"%%%"}
                            """)).build()
            );

            // when
            InboxException exception = assertThrows(
                    InboxException.class,
                    () -> service.download(createMailAccount(), createMessage(), createAttachment())
            );

            // then
            assertEquals("MS-INBOX-ATTACHMENT-SOURCE-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("RestClient 호출이 실패하면 다운로드 실패 예외를 반환한다")
        void download_restClient호출이실패하면다운로드실패예외를반환한다() {
            // given
            GoogleMailProperties properties = new GoogleMailProperties();
            properties.setAttachmentsUri("https://gmail.googleapis.com/gmail/v1/users/me/messages");
            GoogleMailAttachmentQueryService service = new GoogleMailAttachmentQueryService(
                    properties,
                    RestClient.builder().requestFactory(new FailingClientHttpRequestFactory()).build()
            );

            // when
            InboxException exception = assertThrows(
                    InboxException.class,
                    () -> service.download(createMailAccount(), createMessage(), createAttachment())
            );

            // then
            assertEquals("MS-INBOX-ATTACHMENT-DOWNLOAD-FAILED", exception.getErrorCode().getCode());
        }
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@gmail.com")
                .alias("업무 메일")
                .icon("mail")
                .color("#123ABC")
                .accessToken("access-token")
                .build();
    }

    private Message createMessage() {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(Thread.builder()
                        .id(UUID.randomUUID())
                        .mailAccount(createMailAccount())
                        .gmailThreadId("gmail-thread-id")
                        .build())
                .gmailMessageId("gmail-message-id")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
    }

    private Attachment createAttachment() {
        return Attachment.builder()
                .id(UUID.randomUUID())
                .gmailAttachmentId("attachment-1")
                .filename("guide.pdf")
                .mimeType("application/pdf")
                .size(5)
                .build();
    }

    private static final class CapturingClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        private final String responseBody;
        private URI requestUri;

        private CapturingClientHttpRequestFactory(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            requestUri = uri;
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    if (responseBody.isBlank()) {
                        return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
                    }
                    MockClientHttpResponse response = new MockClientHttpResponse(
                            responseBody.getBytes(StandardCharsets.UTF_8),
                            HttpStatus.OK
                    );
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }
            };
        }

        private String requestUri() {
            return requestUri.toString();
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
