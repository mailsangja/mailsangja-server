package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailSendResponse;
import com.mailsangja.core.dto.mail.GoogleMailSendResult;
import com.mailsangja.core.dto.mail.MailAttachmentCommand;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.db.entity.mail.MailAccount;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
public class GoogleMailSendCommandService {

    private final GoogleMailProperties googleMailProperties;
    private final RestClient googleMailRestClient;

    public GoogleMailSendCommandService(
            GoogleMailProperties googleMailProperties,
            @Qualifier("googleMailRestClient") RestClient googleMailRestClient
    ) {
        this.googleMailProperties = googleMailProperties;
        this.googleMailRestClient = googleMailRestClient;
    }

    public GoogleMailSendResult send(MailAccount mailAccount, MailSendCommand command) {
        validateInput(mailAccount, command);

        String rawMessage = createRawMessage(command);

        try {
            GoogleMailSendResponse response = googleMailRestClient
                    .post()
                    .uri(googleMailProperties.getSendUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mailAccount.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("raw", rawMessage))
                    .retrieve()
                    .body(GoogleMailSendResponse.class);

            return validateResponse(response);
        } catch (RestClientException e) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_SEND_FAILED);
        }
    }

    private void validateInput(MailAccount mailAccount, MailSendCommand command) {
        if (mailAccount == null
                || command == null
                || isBlank(mailAccount.getAccessToken())
                || isBlank(googleMailProperties.getSendUri())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_SEND_FAILED);
        }
    }

    private GoogleMailSendResult validateResponse(GoogleMailSendResponse response) {
        if (response == null || isBlank(response.id()) || isBlank(response.threadId())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_SEND_RESULT_INVALID);
        }

        return new GoogleMailSendResult(
                response.id(),
                response.threadId(),
                response.historyId()
        );
    }

    private String createRawMessage(MailSendCommand command) {
        try {
            Session session = Session.getInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(command.from()));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", command.to())));

            if (command.cc() != null && !command.cc().isEmpty()) {
                mimeMessage.setRecipients(Message.RecipientType.CC, InternetAddress.parse(String.join(",", command.cc())));
            }

            if (command.bcc() != null && !command.bcc().isEmpty()) {
                mimeMessage.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(String.join(",", command.bcc())));
            }

            String normalizedSubject = normalizeSubject(command.subject());
            if (!isBlank(normalizedSubject)) {
                mimeMessage.setSubject(normalizedSubject, StandardCharsets.UTF_8.name());
            }

            if (command.attachments() == null || command.attachments().isEmpty()) {
                mimeMessage.setText(command.content() == null ? "" : command.content(), StandardCharsets.UTF_8.name());
            } else {
                MimeMultipart multipart = new MimeMultipart();

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(command.content() == null ? "" : command.content(), StandardCharsets.UTF_8.name());
                multipart.addBodyPart(textPart);

                for (MailAttachmentCommand attachment : command.attachments()) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.setDataHandler(new jakarta.activation.DataHandler(
                            new ByteArrayDataSource(
                                    attachment.bytes(),
                                    attachment.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : attachment.contentType()
                            )
                    ));
                    attachmentPart.setFileName(attachment.filename());
                    multipart.addBodyPart(attachmentPart);
                }

                mimeMessage.setContent(multipart);
            }

            mimeMessage.saveChanges();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(outputStream.toByteArray());
        } catch (MessagingException | java.io.IOException e) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_SEND_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeSubject(String subject) {
        if (subject == null) {
            return null;
        }

        return subject.replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }
}
