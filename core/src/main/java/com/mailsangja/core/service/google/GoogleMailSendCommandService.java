package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailReplyContextResult;
import com.mailsangja.core.dto.mail.GoogleMailSendResponse;
import com.mailsangja.core.dto.mail.GoogleMailSendResult;
import com.mailsangja.core.dto.mail.MailAddressCommand;
import com.mailsangja.core.dto.mail.MailAttachmentCommand;
import com.mailsangja.core.dto.mail.MailInlineImageCommand;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.db.entity.mail.MailAccount;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
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

    public GoogleMailSendResult reply(
            MailAccount mailAccount,
            MailSendCommand command,
            GoogleMailReplyContextResult replyContext
    ) {
        validateInput(mailAccount, command);
        validateReplyContext(replyContext);

        String rawMessage = createRawReplyMessage(command, replyContext);

        try {
            GoogleMailSendResponse response = googleMailRestClient
                    .post()
                    .uri(googleMailProperties.getSendUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mailAccount.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "raw", rawMessage,
                            "threadId", replyContext.gmailThreadId()
                    ))
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
                response.threadId()
        );
    }

    private String createRawMessage(MailSendCommand command) {
        try {
            return encodeMimeMessage(createMimeMessage(command));
        } catch (MailSendException e) {
            throw e;
        } catch (MessagingException | java.io.IOException e) {
            throw new MailSendException(MailSendErrorCode.MAIL_MIME_BUILD_FAILED);
        }
    }

    private String createRawReplyMessage(MailSendCommand command, GoogleMailReplyContextResult replyContext) {
        try {
            MimeMessage mimeMessage = createMimeMessage(command);
            mimeMessage.setHeader("In-Reply-To", replyContext.parentRfcMessageId());
            mimeMessage.setHeader("References", createReferencesHeader(replyContext));
            return encodeMimeMessage(mimeMessage);
        } catch (MailSendException e) {
            throw e;
        } catch (MessagingException | java.io.IOException e) {
            throw new MailSendException(MailSendErrorCode.MAIL_MIME_BUILD_FAILED);
        }
    }

    private MimeMessage createMimeMessage(MailSendCommand command) throws MessagingException {
        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(createInternetAddress(command.from(), MailSendErrorCode.INVALID_SENDER_ADDRESS));
        if (command.replyTo() != null) {
            mimeMessage.setReplyTo(new InternetAddress[]{
                    createInternetAddress(command.replyTo(), MailSendErrorCode.INVALID_REPLY_TO_ADDRESS)
            });
        }
        mimeMessage.setRecipients(Message.RecipientType.TO, createInternetAddresses(command.to()));

        if (command.cc() != null && !command.cc().isEmpty()) {
            mimeMessage.setRecipients(Message.RecipientType.CC, createInternetAddresses(command.cc()));
        }

        if (command.bcc() != null && !command.bcc().isEmpty()) {
            mimeMessage.setRecipients(Message.RecipientType.BCC, createInternetAddresses(command.bcc()));
        }

        String normalizedSubject = normalizeSubject(command.subject());
        if (!isBlank(normalizedSubject)) {
            mimeMessage.setSubject(normalizedSubject, StandardCharsets.UTF_8.name());
        }

        setMessageContent(mimeMessage, command);

        return mimeMessage;
    }

    private void setMessageContent(MimeMessage mimeMessage, MailSendCommand command) throws MessagingException {
        boolean hasAttachments = command.attachments() != null && !command.attachments().isEmpty();
        boolean hasInlineImages = command.inlineImages() != null && !command.inlineImages().isEmpty();

        if (!hasAttachments && !hasInlineImages) {
            mimeMessage.setContent(normalizeContent(command.content()), "text/html; charset=UTF-8");
            return;
        }

        if (!hasAttachments) {
            mimeMessage.setContent(createRelatedMultipart(command));
            return;
        }

        MimeMultipart mixedMultipart = new MimeMultipart("mixed");
        if (hasInlineImages) {
            MimeBodyPart relatedPart = new MimeBodyPart();
            relatedPart.setContent(createRelatedMultipart(command));
            mixedMultipart.addBodyPart(relatedPart);
        } else {
            mixedMultipart.addBodyPart(createHtmlBodyPart(command.content()));
        }

        for (MailAttachmentCommand attachment : command.attachments()) {
            mixedMultipart.addBodyPart(createAttachmentBodyPart(attachment));
        }
        mimeMessage.setContent(mixedMultipart);
    }

    private MimeMultipart createRelatedMultipart(MailSendCommand command) throws MessagingException {
        MimeMultipart relatedMultipart = new MimeMultipart("related");
        relatedMultipart.addBodyPart(createHtmlBodyPart(command.content()));

        for (MailInlineImageCommand inlineImage : command.inlineImages()) {
            relatedMultipart.addBodyPart(createInlineImageBodyPart(inlineImage));
        }
        return relatedMultipart;
    }

    private MimeBodyPart createHtmlBodyPart(String content) throws MessagingException {
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(normalizeContent(content), "text/html; charset=UTF-8");
        return htmlPart;
    }

    private MimeBodyPart createInlineImageBodyPart(MailInlineImageCommand inlineImage) throws MessagingException {
        MimeBodyPart inlineImagePart = new MimeBodyPart();
        inlineImagePart.setDataHandler(new DataHandler(
                new ByteArrayDataSource(
                        inlineImage.bytes(),
                        inlineImage.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : inlineImage.contentType()
                )
        ));
        inlineImagePart.setFileName(inlineImage.filename());
        inlineImagePart.setDisposition(MimeBodyPart.INLINE);
        inlineImagePart.setHeader("Content-ID", "<" + inlineImage.cid() + ">");
        return inlineImagePart;
    }

    private MimeBodyPart createAttachmentBodyPart(MailAttachmentCommand attachment) throws MessagingException {
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setDataHandler(new DataHandler(
                new ByteArrayDataSource(
                        attachment.bytes(),
                        attachment.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : attachment.contentType()
                )
        ));
        attachmentPart.setFileName(attachment.filename());
        return attachmentPart;
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content;
    }

    private String encodeMimeMessage(MimeMessage mimeMessage) throws MessagingException, java.io.IOException {
        mimeMessage.saveChanges();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mimeMessage.writeTo(outputStream);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(outputStream.toByteArray());
    }

    private InternetAddress createInternetAddress(MailAddressCommand addressCommand, MailSendErrorCode errorCode) {
        try {
            String address = addressCommand.address();
            String name = normalizeDisplayName(addressCommand.name());

            if (isBlank(name) || address.equalsIgnoreCase(name)) {
                return new InternetAddress(address, true);
            }

            return new InternetAddress(address, name, StandardCharsets.UTF_8.name());
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException(errorCode);
        }
    }

    private InternetAddress[] createInternetAddresses(List<MailAddressCommand> addresses) {
        try {
            InternetAddress[] internetAddresses = new InternetAddress[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                MailAddressCommand addressCommand = addresses.get(i);
                String name = normalizeDisplayName(addressCommand.name());

                if (isBlank(name) || addressCommand.address().equalsIgnoreCase(name)) {
                    internetAddresses[i] = new InternetAddress(addressCommand.address(), true);
                    continue;
                }

                internetAddresses[i] = new InternetAddress(
                        addressCommand.address(),
                        name,
                        StandardCharsets.UTF_8.name()
                );
            }
            return internetAddresses;
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException(MailSendErrorCode.INVALID_RECIPIENT_ADDRESS);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateReplyContext(GoogleMailReplyContextResult replyContext) {
        if (replyContext == null
                || isBlank(replyContext.gmailThreadId())
                || isBlank(replyContext.parentRfcMessageId())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_SEND_FAILED);
        }
    }

    private String createReferencesHeader(GoogleMailReplyContextResult replyContext) {
        if (isBlank(replyContext.referencesHeader())) {
            return replyContext.parentRfcMessageId();
        }

        return replyContext.referencesHeader().trim() + " " + replyContext.parentRfcMessageId();
    }

    private String normalizeSubject(String subject) {
        if (subject == null) {
            return null;
        }

        return subject.replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private String normalizeDisplayName(String name) {
        if (name == null) {
            return null;
        }

        String normalizedName = name.replace("\r", " ")
                .replace("\n", " ")
                .trim();
        return normalizedName.isEmpty() ? null : normalizedName;
    }
}
