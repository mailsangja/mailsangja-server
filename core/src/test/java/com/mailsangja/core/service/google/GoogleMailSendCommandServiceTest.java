package com.mailsangja.core.service.google;

import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.MailAddressCommand;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.db.entity.mail.MailAccount;
import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GoogleMailSendCommandService 테스트")
class GoogleMailSendCommandServiceTest {

    @Nested
    @DisplayName("메일 전송")
    class Send {

        @Test
        @DisplayName("이름이 포함된 발신자와 수신자 헤더를 생성한다")
        void send_이름이포함된발신자와수신자헤더를생성한다() throws Exception {
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
                    List.of(new MailAddressCommand("개발팀", "cc@example.com")),
                    List.of(new MailAddressCommand(null, "bcc@example.com")),
                    "제목",
                    "본문",
                    List.of()
            );

            // when
            service.send(mailAccount, command);

            // then
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
    }

    private void assertAddress(Address actualAddress, String expectedName, String expectedAddress) throws Exception {
        InternetAddress internetAddress = (InternetAddress) actualAddress;
        assertEquals(expectedName, internetAddress.getPersonal());
        assertEquals(expectedAddress, internetAddress.getAddress());
    }

    private String extractRawMessage(String requestBody) {
        Pattern pattern = Pattern.compile("\"raw\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(requestBody);
        assertNotNull(requestBody);
        assertTrue(matcher.find());
        return matcher.group(1);
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
                            """
                                    {"id":"gmail-message-id","threadId":"gmail-thread-id"}
                                    """.getBytes(StandardCharsets.UTF_8),
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
}
