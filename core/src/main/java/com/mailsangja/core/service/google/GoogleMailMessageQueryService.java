package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
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

        return new GoogleMailMessageResult(
                response.id(),
                response.threadId(),
                response.historyId(),
                extractHeaderValue(response, "Subject"),
                extractRequiredAddress(response, "From"),
                extractAddresses(response, "To"),
                extractAddresses(response, "Cc"),
                response.snippet(),
                resolveSentAt(response.internalDate()),
                bodyContent.text(),
                bodyContent.html()
        );
    }

    private String extractRequiredAddress(GoogleMailMessageResponse response, String headerName) {
        List<String> addresses = extractAddresses(response, headerName);
        if (addresses.isEmpty()) {
            throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
        }
        return addresses.getFirst();
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

    private List<String> extractAddresses(GoogleMailMessageResponse response, String headerName) {
        String headerValue = extractHeaderValue(response, headerName);
        if (isBlank(headerValue)) {
            return Collections.emptyList();
        }

        try {
            InternetAddress[] addresses = InternetAddress.parseHeader(headerValue, true);
            List<String> normalizedAddresses = new ArrayList<>();
            for (InternetAddress address : addresses) {
                if (address != null && !isBlank(address.getAddress())) {
                    normalizedAddresses.add(address.getAddress().trim().toLowerCase());
                }
            }
            return List.copyOf(normalizedAddresses);
        } catch (AddressException e) {
            if ("From".equalsIgnoreCase(headerName)) {
                throw new MailSendException(MailSendErrorCode.GOOGLE_MAIL_MESSAGE_RESULT_INVALID);
            }
            return Collections.emptyList();
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record MimeBodyContent(
            String text,
            String html
    ) {
    }
}
