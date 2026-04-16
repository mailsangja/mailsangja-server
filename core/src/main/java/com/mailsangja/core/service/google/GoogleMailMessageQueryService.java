package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailAttachmentResult;
import com.mailsangja.core.dto.mail.GoogleMailMessageResponse;
import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleMailMessageQueryService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final GoogleMailProperties googleMailProperties;
    private final RestClient googleMailRestClient;

    public GoogleMailMessageQueryService(
            GoogleMailProperties googleMailProperties,
            @Qualifier("googleMailRestClient") RestClient googleMailRestClient
    ) {
        this.googleMailProperties = googleMailProperties;
        this.googleMailRestClient = googleMailRestClient;
    }

    public GoogleMailMessageResult getMessage(String accessToken, String gmailMessageId) {
        validateInput(accessToken, gmailMessageId);

        try {
            GoogleMailMessageResponse response = googleMailRestClient
                    .get()
                    .uri(buildMessageUri(gmailMessageId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleMailMessageResponse.class);

            return toMessageResult(validateResponse(response));
        } catch (RestClientException e) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_FETCH_FAILED);
        }
    }

    private void validateInput(String accessToken, String gmailMessageId) {
        if (isBlank(accessToken)
                || isBlank(gmailMessageId)
                || isBlank(googleMailProperties.getMessagesUri())) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_FETCH_FAILED);
        }
    }

    private String buildMessageUri(String gmailMessageId) {
        return UriComponentsBuilder.fromUriString(googleMailProperties.getMessagesUri())
                .pathSegment(gmailMessageId)
                .queryParam("format", "full")
                .build()
                .toUriString();
    }

    private GoogleMailMessageResponse validateResponse(GoogleMailMessageResponse response) {
        if (response == null || isBlank(response.id()) || isBlank(response.threadId()) || response.payload() == null) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }

        return response;
    }

    private GoogleMailMessageResult toMessageResult(GoogleMailMessageResponse response) {
        MimeBodyContent bodyContent = extractBodyContent(response.payload());
        ParsedMailAddresses from = extractRequiredMailAddresses(response, "From");
        ParsedMailAddresses to = extractMailAddresses(response, "To");
        ParsedMailAddresses cc = extractMailAddresses(response, "Cc");

        return new GoogleMailMessageResult(
                response.id(),
                response.threadId(),
                response.historyId(),
                extractHeaderValue(response, "Subject"),
                from.addresses().getFirst(),
                from.names().getFirst(),
                to.addresses(),
                to.names(),
                cc.addresses(),
                cc.names(),
                response.snippet(),
                resolveSentAt(response.internalDate()),
                bodyContent.text(),
                bodyContent.html(),
                createAttachments(response.payload())
        );
    }

    private ParsedMailAddresses extractRequiredMailAddresses(GoogleMailMessageResponse response, String headerName) {
        ParsedMailAddresses addresses = extractMailAddresses(response, headerName);
        if (addresses.addresses().isEmpty()) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }
        return addresses;
    }

    private String extractHeaderValue(GoogleMailMessageResponse response, String headerName) {
        if (response.payload() == null || response.payload().headers() == null) {
            return null;
        }

        return response.payload().headers().stream()
                .filter(header -> header != null
                        && !isBlank(header.name())
                        && headerName.equalsIgnoreCase(header.name()))
                .map(GoogleMailMessageResponse.GoogleMailHeaderResponse::value)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private ParsedMailAddresses extractMailAddresses(GoogleMailMessageResponse response, String headerName) {
        String headerValue = extractHeaderValue(response, headerName);
        if (isBlank(headerValue)) {
            return ParsedMailAddresses.empty();
        }

        try {
            InternetAddress[] addresses = InternetAddress.parseHeader(headerValue, true);
            List<String> normalizedAddresses = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (InternetAddress address : addresses) {
                if (address != null && !isBlank(address.getAddress())) {
                    normalizedAddresses.add(address.getAddress().trim().toLowerCase());
                    names.add(normalizePersonalName(address.getPersonal()));
                }
            }
            return ParsedMailAddresses.of(normalizedAddresses, names);
        } catch (AddressException e) {
            if ("From".equalsIgnoreCase(headerName)) {
                throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
            }
            return ParsedMailAddresses.empty();
        }
    }

    private LocalDateTime resolveSentAt(String internalDate) {
        if (isBlank(internalDate)) {
            return null;
        }

        try {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(internalDate)),
                    KST_ZONE_ID
            );
        } catch (NumberFormatException e) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }
    }

    private MimeBodyContent extractBodyContent(GoogleMailMessageResponse.GoogleMailPayloadResponse payload) {
        if (payload == null) {
            return new MimeBodyContent(null, null);
        }

        String text = decodeBody(findBodyData(payload, "text/plain"));
        String html = decodeBody(findBodyData(payload, "text/html"));
        return new MimeBodyContent(text, html);
    }

    private String findBodyData(GoogleMailMessageResponse.GoogleMailPayloadResponse payload, String mimeType) {
        if (payload == null) {
            return null;
        }

        if (mimeType.equalsIgnoreCase(payload.mimeType())
                && payload.body() != null
                && !isBlank(payload.body().data())) {
            return payload.body().data();
        }

        if (payload.parts() == null || payload.parts().isEmpty()) {
            return null;
        }

        for (GoogleMailMessageResponse.GoogleMailPayloadResponse part : payload.parts()) {
            String data = findBodyData(part, mimeType);
            if (!isBlank(data)) {
                return data;
            }
        }

        return null;
    }

    private String decodeBody(String encodedData) {
        if (isBlank(encodedData)) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedData);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }
    }

    private List<GoogleMailAttachmentResult> createAttachments(
            GoogleMailMessageResponse.GoogleMailPayloadResponse payload
    ) {
        List<GoogleMailMessageResponse.GoogleMailPayloadResponse> attachmentParts = new ArrayList<>();
        collectAttachmentParts(payload, attachmentParts);

        return attachmentParts.stream()
                .map(part -> new GoogleMailAttachmentResult(
                        part.body() == null ? null : part.body().attachmentId(),
                        part.filename(),
                        part.mimeType(),
                        part.body() == null ? null : part.body().size()
                ))
                .toList();
    }

    private void collectAttachmentParts(
            GoogleMailMessageResponse.GoogleMailPayloadResponse payload,
            List<GoogleMailMessageResponse.GoogleMailPayloadResponse> attachmentParts
    ) {
        if (payload == null) {
            return;
        }

        if (!isBlank(payload.filename())
                && payload.body() != null
                && !isBlank(payload.body().attachmentId())) {
            attachmentParts.add(payload);
        }

        if (payload.parts() == null || payload.parts().isEmpty()) {
            return;
        }

        payload.parts().forEach(part -> collectAttachmentParts(part, attachmentParts));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizePersonalName(String personalName) {
        if (personalName == null || personalName.isBlank()) {
            return null;
        }
        return personalName.trim();
    }

    private record MimeBodyContent(
            String text,
            String html
    ) {
    }

    private record ParsedMailAddresses(
            List<String> addresses,
            List<String> names
    ) {
        private static ParsedMailAddresses empty() {
            return new ParsedMailAddresses(List.of(), List.of());
        }

        private static ParsedMailAddresses of(List<String> addresses, List<String> names) {
            return new ParsedMailAddresses(
                    Collections.unmodifiableList(new ArrayList<>(addresses)),
                    Collections.unmodifiableList(new ArrayList<>(names))
            );
        }
    }
}
