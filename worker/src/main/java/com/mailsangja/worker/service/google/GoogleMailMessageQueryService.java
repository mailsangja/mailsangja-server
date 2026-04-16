package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.gmail.message.GoogleMailMessageListResponse;
import com.mailsangja.worker.dto.gmail.message.GoogleMailMessageListResult;
import com.mailsangja.worker.dto.gmail.message.GoogleMailThreadResponse;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncAttachmentResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class GoogleMailMessageQueryService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final GoogleMailInitialSyncProperties googleMailInitialSyncProperties;
    private final RestClient googleMailMessageRestClient;

    public GoogleMailMessageQueryService(
            GoogleMailInitialSyncProperties googleMailInitialSyncProperties,
            @Qualifier("googleMailMessageRestClient") RestClient googleMailMessageRestClient
    ) {
        this.googleMailInitialSyncProperties = googleMailInitialSyncProperties;
        this.googleMailMessageRestClient = googleMailMessageRestClient;
    }

    public GoogleMailMessageListResult getLatestMessages(String accessToken) {
        validateInput(accessToken);

        try {
            GoogleMailMessageListResponse response = googleMailMessageRestClient
                    .get()
                    .uri(buildMessagesUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleMailMessageListResponse.class);

            return validateResponse(response);
        } catch (RestClientException e) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }
    }

    public List<InitialMailSyncThreadResult> getThreads(String accessToken, List<String> threadIds) {
        validateThreadInput(accessToken, threadIds);

        return threadIds.stream()
                .map(threadId -> toThreadResult(getThread(accessToken, threadId)))
                .toList();
    }

    private void validateInput(String accessToken) {
        if (isBlank(accessToken)
                || isBlank(googleMailInitialSyncProperties.getMessagesUri())
                || googleMailInitialSyncProperties.getMaxResults() <= 0
                || googleMailInitialSyncProperties.getThreadBatchSize() <= 0) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }
    }

    private String buildMessagesUri() {
        return UriComponentsBuilder.fromUriString(googleMailInitialSyncProperties.getMessagesUri())
                .queryParam("maxResults", googleMailInitialSyncProperties.getMaxResults())
                .build()
                .toUriString();
    }

    private GoogleMailThreadResponse getThread(String accessToken, String threadId) {
        try {
            GoogleMailThreadResponse response = googleMailMessageRestClient
                    .get()
                    .uri(buildThreadUri(threadId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleMailThreadResponse.class);

            return validateThreadResponse(response);
        } catch (RestClientException e) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }
    }

    private String buildThreadUri(String threadId) {
        return UriComponentsBuilder.fromUriString(googleMailInitialSyncProperties.getThreadsUri())
                .pathSegment(threadId)
                .build()
                .toUriString();
    }

    private GoogleMailMessageListResult validateResponse(GoogleMailMessageListResponse response) {
        if (response == null) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        if (response.messages() != null && response.messages().stream()
                .anyMatch(message -> message == null || isBlank(message.id()))) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        int fetchedCount = response.messages() == null ? 0 : response.messages().size();
        int resultSizeEstimate = response.resultSizeEstimate() == null ? fetchedCount : response.resultSizeEstimate();

        return new GoogleMailMessageListResult(response.messages(), resultSizeEstimate);
    }

    private void validateThreadInput(String accessToken, List<String> threadIds) {
        if (isBlank(accessToken)
                || isBlank(googleMailInitialSyncProperties.getThreadsUri())
                || threadIds == null
                || threadIds.isEmpty()
                || threadIds.stream().anyMatch(this::isBlank)) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }
    }

    private GoogleMailThreadResponse validateThreadResponse(GoogleMailThreadResponse response) {
        if (response == null
                || isBlank(response.id())
                || response.messages() == null
                || response.messages().isEmpty()) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        return response;
    }

    private InitialMailSyncThreadResult toThreadResult(GoogleMailThreadResponse response) {
        return new InitialMailSyncThreadResult(
                response.id(),
                response.historyId(),
                response.messages().stream()
                        .map(message -> toMessageResult(response, message))
                        .toList()
        );
    }

    private InitialMailSyncMessageResult toMessageResult(
            GoogleMailThreadResponse threadResponse,
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse
    ) {
        validateThreadMessage(threadResponse, messageResponse);
        MimeBodyContent bodyContent = extractBodyContent(messageResponse.payload());
        ParsedMailAddresses from = extractRequiredMailAddresses(messageResponse, "From");
        ParsedMailAddresses to = extractMailAddresses(messageResponse, "To");
        ParsedMailAddresses cc = extractMailAddresses(messageResponse, "Cc");

        return new InitialMailSyncMessageResult(
                messageResponse.id(),
                messageResponse.threadId(),
                firstNonBlank(messageResponse.historyId(), threadResponse.historyId()),
                resolveDirection(messageResponse.labelIds()),
                extractHeaderValue(messageResponse, "Subject"),
                from.addresses().getFirst(),
                from.names().getFirst(),
                to.addresses(),
                to.names(),
                cc.addresses(),
                cc.names(),
                messageResponse.snippet(),
                isRead(messageResponse.labelIds()),
                resolveSentAt(messageResponse),
                bodyContent.text(),
                bodyContent.html(),
                createAttachments(messageResponse.payload())
        );
    }

    private void validateThreadMessage(
            GoogleMailThreadResponse threadResponse,
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse
    ) {
        if (messageResponse == null
                || isBlank(messageResponse.id())
                || isBlank(messageResponse.threadId())
                || !threadResponse.id().equals(messageResponse.threadId())) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
    }

    private com.mailsangja.db.entity.mail.Direction resolveDirection(List<String> labelIds) {
        return labelIds != null && labelIds.contains("SENT")
                ? com.mailsangja.db.entity.mail.Direction.OUTBOUND
                : com.mailsangja.db.entity.mail.Direction.INBOUND;
    }

    private boolean isRead(List<String> labelIds) {
        return labelIds == null || !labelIds.contains("UNREAD");
    }

    private ParsedMailAddresses extractRequiredMailAddresses(
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse,
            String headerName
    ) {
        ParsedMailAddresses addresses = extractMailAddresses(messageResponse, headerName);
        if (addresses.addresses().isEmpty()) {
            log.warn(
                    "Failed to normalize required mail header. threadId={} gmailMessageId={} headerName={}",
                    messageResponse.threadId(),
                    messageResponse.id(),
                    headerName
            );
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
        return addresses;
    }

    private String extractHeaderValue(
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse,
            String headerName
    ) {
        if (messageResponse.payload() == null || messageResponse.payload().headers() == null) {
            return null;
        }

        return messageResponse.payload().headers().stream()
                .filter(header -> header != null
                        && !isBlank(header.name())
                        && headerName.equalsIgnoreCase(header.name()))
                .map(GoogleMailThreadResponse.GoogleMailHeaderResponse::value)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private ParsedMailAddresses extractMailAddresses(
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse,
            String headerName
    ) {
        String headerValue = extractHeaderValue(messageResponse, headerName);
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
            log.warn(
                    "Failed to normalize mail header. threadId={} gmailMessageId={} headerName={} headerValue={}",
                    messageResponse.threadId(),
                    messageResponse.id(),
                    headerName,
                    headerValue
            );
            if ("From".equalsIgnoreCase(headerName)) {
                throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
            }
            return ParsedMailAddresses.empty();
        }
    }

    private LocalDateTime resolveSentAt(GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse) {
        if (isBlank(messageResponse.internalDate())) {
            return null;
        }

        try {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(messageResponse.internalDate())),
                    KST_ZONE_ID
            );
        } catch (NumberFormatException e) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
    }

    private MimeBodyContent extractBodyContent(GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload) {
        if (payload == null) {
            return new MimeBodyContent(null, null);
        }

        String text = decodeBody(findBodyData(payload, "text/plain"));
        String html = decodeBody(findBodyData(payload, "text/html"));
        return new MimeBodyContent(text, html);
    }

    private String findBodyData(GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload, String mimeType) {
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

        for (GoogleMailThreadResponse.GoogleMailThreadPayloadResponse part : payload.parts()) {
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
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
    }

    private List<InitialMailSyncAttachmentResult> createAttachments(
            GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload
    ) {
        List<GoogleMailThreadResponse.GoogleMailThreadPayloadResponse> attachmentParts = new ArrayList<>();
        collectAttachmentParts(payload, attachmentParts);

        return attachmentParts.stream()
                .map(part -> new InitialMailSyncAttachmentResult(
                        part.body() == null ? null : part.body().attachmentId(),
                        part.filename(),
                        part.mimeType(),
                        part.body() == null ? null : part.body().size()
                ))
                .toList();
    }

    private void collectAttachmentParts(
            GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload,
            List<GoogleMailThreadResponse.GoogleMailThreadPayloadResponse> attachmentParts
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

    private String firstNonBlank(String primary, String secondary) {
        return !isBlank(primary) ? primary : secondary;
    }

    private String normalizePersonalName(String personalName) {
        if (isBlank(personalName)) {
            return null;
        }
        return personalName.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
