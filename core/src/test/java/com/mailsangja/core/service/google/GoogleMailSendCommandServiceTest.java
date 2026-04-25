package com.mailsangja.core.service.google;

import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailReplyContextResult;
import com.mailsangja.core.dto.mail.MailAddressCommand;
import com.mailsangja.core.dto.mail.MailAttachmentCommand;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.db.entity.mail.MailAccount;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleMailSendCommandServiceTest {

    @Test
    void send_이름이포함된발신자와수신자헤더를생성한다() throws Exception {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setSendUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

        GoogleMailSendCommandService service = new GoogleMailSendCommandService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .accessToken("access-token")
                .build();

        MailSendCommand command = new MailSendCommand(
                UUID.randomUUID(),
                new MailAddressCommand("홍길동", "sender@example.com"),
                List.of(new MailAddressCommand("김철수", "to@example.com")),
                List.of(new MailAddressCommand("개발팀", "cc@example.com")),
                List.of(new MailAddressCommand(null, "bcc@example.com")),
                "제목",
                "본문",
                List.of()
        );

        service.send(mailAccount, command);

        String raw = extractRawMessage(requestFactory.requestBody());
        MimeMessage mimeMessage = new MimeMessage(
                Session.getInstance(new Properties()),
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(raw))
        );

        assertAddress(mimeMessage.getFrom()[0], "홍길동", "sender@example.com");
        assertAddress(mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.TO)[0], "김철수", "to@example.com");
        assertAddress(mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.CC)[0], "개발팀", "cc@example.com");
        assertAddress(mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.BCC)[0], null, "bcc@example.com");
    }

    @Test
    void reply_답장헤더와threadId를포함해전송한다() throws Exception {
        // given
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setSendUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();

        GoogleMailSendCommandService service = new GoogleMailSendCommandService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );

        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .accessToken("access-token")
                .build();

        MailSendCommand command = new MailSendCommand(
                UUID.randomUUID(),
                new MailAddressCommand("홍길동", "sender@example.com"),
                List.of(new MailAddressCommand("김철수", "to@example.com")),
                List.of(),
                List.of(),
                "Re: 제목",
                "답장 본문",
                List.of()
        );

        // when
        service.reply(
                mailAccount,
                command,
                new GoogleMailReplyContextResult(
                        "gmail-thread-id",
                        "<parent-message@example.com>",
                        "<older-message@example.com>",
                        "Re: 제목"
                )
        );

        // then
        String raw = extractJsonField(requestFactory.requestBody(), "raw");
        MimeMessage mimeMessage = new MimeMessage(
                Session.getInstance(new Properties()),
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(raw))
        );

        assertEquals("<parent-message@example.com>", mimeMessage.getHeader("In-Reply-To", null));
        assertEquals(
                "<older-message@example.com> <parent-message@example.com>",
                mimeMessage.getHeader("References", null)
        );
        assertEquals("gmail-thread-id", extractJsonField(requestFactory.requestBody(), "threadId"));
    }

    @Test
    void reply_referencesHeader가없으면부모MessageId만References에설정한다() throws Exception {
        // given
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();
        GoogleMailSendCommandService service = createService(requestFactory);
        MailAccount mailAccount = createMailAccount();
        MailSendCommand command = createReplyCommand(List.of(), List.of(), List.of());

        // when
        service.reply(
                mailAccount,
                command,
                new GoogleMailReplyContextResult(
                        "gmail-thread-id",
                        "<parent-message@example.com>",
                        null,
                        "Re: 제목"
                )
        );

        // then
        MimeMessage mimeMessage = extractMimeMessage(requestFactory.requestBody());
        assertEquals("<parent-message@example.com>", mimeMessage.getHeader("In-Reply-To", null));
        assertEquals("<parent-message@example.com>", mimeMessage.getHeader("References", null));
    }

    @Test
    void reply_referencesHeader가blank면부모MessageId만References에설정한다() throws Exception {
        // given
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();
        GoogleMailSendCommandService service = createService(requestFactory);
        MailAccount mailAccount = createMailAccount();
        MailSendCommand command = createReplyCommand(List.of(), List.of(), List.of());

        // when
        service.reply(
                mailAccount,
                command,
                new GoogleMailReplyContextResult(
                        "gmail-thread-id",
                        "<parent-message@example.com>",
                        " ",
                        "Re: 제목"
                )
        );

        // then
        MimeMessage mimeMessage = extractMimeMessage(requestFactory.requestBody());
        assertEquals("<parent-message@example.com>", mimeMessage.getHeader("In-Reply-To", null));
        assertEquals("<parent-message@example.com>", mimeMessage.getHeader("References", null));
    }

    @Test
    void reply_cc와bcc가있으면수신자헤더를포함한다() throws Exception {
        // given
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();
        GoogleMailSendCommandService service = createService(requestFactory);
        MailAccount mailAccount = createMailAccount();
        MailSendCommand command = createReplyCommand(
                List.of(new MailAddressCommand("개발팀", "cc@example.com")),
                List.of(new MailAddressCommand(null, "bcc@example.com")),
                List.of()
        );

        // when
        service.reply(
                mailAccount,
                command,
                new GoogleMailReplyContextResult(
                        "gmail-thread-id",
                        "<parent-message@example.com>",
                        null,
                        "Re: 제목"
                )
        );

        // then
        MimeMessage mimeMessage = extractMimeMessage(requestFactory.requestBody());
        assertAddress(mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.CC)[0], "개발팀", "cc@example.com");
        assertAddress(mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.BCC)[0], null, "bcc@example.com");
    }

    @Test
    void reply_첨부파일이있으면multipart로전송한다() throws Exception {
        // given
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory();
        GoogleMailSendCommandService service = createService(requestFactory);
        MailAccount mailAccount = createMailAccount();
        MailSendCommand command = createReplyCommand(
                List.of(),
                List.of(),
                List.of(new MailAttachmentCommand("file.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8)))
        );

        // when
        service.reply(
                mailAccount,
                command,
                new GoogleMailReplyContextResult(
                        "gmail-thread-id",
                        "<parent-message@example.com>",
                        null,
                        "Re: 제목"
                )
        );

        // then
        MimeMessage mimeMessage = extractMimeMessage(requestFactory.requestBody());
        MimeMultipart multipart = assertInstanceOf(MimeMultipart.class, mimeMessage.getContent());
        assertEquals(2, multipart.getCount());
        assertTrue(multipart.getBodyPart(0).getContent().toString().contains("답장 본문"));

        BodyPart attachmentPart = multipart.getBodyPart(1);
        assertEquals("file.txt", attachmentPart.getFileName());
        assertTrue(attachmentPart.getContentType().contains("text/plain"));
    }

    @Test
    void reply_googleApi응답본문이유효하지않아도2xx이면성공으로처리한다() {
        // given
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory(
                """
                        {}
                        """,
                HttpStatus.OK
        );
        GoogleMailSendCommandService service = createService(requestFactory);
        MailAccount mailAccount = createMailAccount();
        MailSendCommand command = createReplyCommand(List.of(), List.of(), List.of());
        GoogleMailReplyContextResult replyContext = new GoogleMailReplyContextResult(
                "gmail-thread-id",
                "<parent-message@example.com>",
                null,
                "Re: 제목"
        );

        // when
        org.junit.jupiter.api.function.Executable executable =
                () -> service.reply(mailAccount, command, replyContext);

        // then
        assertDoesNotThrow(executable);
    }

    @Test
    void reply_googleApi호출이실패하면실패한다() {
        // given
        CapturingClientHttpRequestFactory requestFactory = new CapturingClientHttpRequestFactory(
                """
                        {"error":"failed"}
                        """,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        GoogleMailSendCommandService service = createService(requestFactory);
        MailAccount mailAccount = createMailAccount();
        MailSendCommand command = createReplyCommand(List.of(), List.of(), List.of());
        GoogleMailReplyContextResult replyContext = new GoogleMailReplyContextResult(
                "gmail-thread-id",
                "<parent-message@example.com>",
                null,
                "Re: 제목"
        );

        // when
        MailSendException exception = assertThrows(
                MailSendException.class,
                () -> service.reply(mailAccount, command, replyContext)
        );

        // then
        assertEquals(MailSendErrorCode.GOOGLE_MAIL_SEND_FAILED, exception.getErrorCode());
    }

    private void assertAddress(Address actualAddress, String expectedName, String expectedAddress) throws Exception {
        InternetAddress internetAddress = (InternetAddress) actualAddress;
        assertEquals(expectedName, internetAddress.getPersonal());
        assertEquals(expectedAddress, internetAddress.getAddress());
    }

    private GoogleMailSendCommandService createService(CapturingClientHttpRequestFactory requestFactory) {
        GoogleMailProperties properties = new GoogleMailProperties();
        properties.setSendUri("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");
        return new GoogleMailSendCommandService(
                properties,
                RestClient.builder().requestFactory(requestFactory).build()
        );
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .accessToken("access-token")
                .build();
    }

    private MailSendCommand createReplyCommand(
            List<MailAddressCommand> cc,
            List<MailAddressCommand> bcc,
            List<MailAttachmentCommand> attachments
    ) {
        return new MailSendCommand(
                UUID.randomUUID(),
                new MailAddressCommand("홍길동", "sender@example.com"),
                List.of(new MailAddressCommand("김철수", "to@example.com")),
                cc,
                bcc,
                "Re: 제목",
                "답장 본문",
                attachments
        );
    }

    private String extractRawMessage(String requestBody) {
        return extractJsonField(requestBody, "raw");
    }

    private MimeMessage extractMimeMessage(String requestBody) throws Exception {
        String raw = extractJsonField(requestBody, "raw");
        return new MimeMessage(
                Session.getInstance(new Properties()),
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(raw))
        );
    }

    private String extractJsonField(String requestBody, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(requestBody);
        assertNotNull(requestBody);
        org.junit.jupiter.api.Assertions.assertTrue(matcher.find());
        return matcher.group(1);
    }

    private static final class CapturingClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        private final String responseBody;
        private final HttpStatus responseStatus;
        private String requestBody;

        private CapturingClientHttpRequestFactory() {
            this(
                    """
                            {"id":"gmail-message-id","threadId":"gmail-thread-id"}
                            """,
                    HttpStatus.OK
            );
        }

        private CapturingClientHttpRequestFactory(String responseBody, HttpStatus responseStatus) {
            this.responseBody = responseBody;
            this.responseStatus = responseStatus;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() {
                    requestBody = getBodyAsString();
                    MockClientHttpResponse response = new MockClientHttpResponse(
                            responseBody.getBytes(StandardCharsets.UTF_8),
                            responseStatus
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
}
