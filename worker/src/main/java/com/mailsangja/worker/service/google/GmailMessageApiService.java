package com.mailsangja.worker.service.google;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.gmail.GoogleMailApiContext;
import com.mailsangja.worker.dto.gmail.message.GoogleMailThreadListResponse;
import com.mailsangja.worker.dto.gmail.message.GoogleMailThreadResponse;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncAttachmentResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.db.entity.mail.AttachmentDisposition;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class GmailMessageApiService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int MAX_THREAD_LIST_PAGE_SIZE = 500;

    private final GoogleMailInitialSyncProperties googleMailInitialSyncProperties;
    private final RestClient googleMailMessageRestClient;
    private final GmailApiRateLimitService gmailApiRateLimitService;

    public GmailMessageApiService(
            GoogleMailInitialSyncProperties googleMailInitialSyncProperties,
            @Qualifier("googleMailMessageRestClient") RestClient googleMailMessageRestClient,
            GmailApiRateLimitService gmailApiRateLimitService
    ) {
        this.googleMailInitialSyncProperties = googleMailInitialSyncProperties;
        this.googleMailMessageRestClient = googleMailMessageRestClient;
        this.gmailApiRateLimitService = gmailApiRateLimitService;
    }

    public List<String> getInitialThreadIds(GoogleMailApiContext context) {
        validateThreadListInput(context);

        Set<String> threadIds = new LinkedHashSet<>();
        String pageToken = null;
        do {
            GoogleMailThreadListResponse response = requestThreadList(context, pageToken, remainingThreadCount(threadIds));
            appendThreadIds(threadIds, response);
            pageToken = response.nextPageToken();
        } while (threadIds.size() < googleMailInitialSyncProperties.getMaxThreads() && !isBlank(pageToken));

        return threadIds.stream()
                .limit(googleMailInitialSyncProperties.getMaxThreads())
                .toList();
    }

    public List<InitialMailSyncThreadResult> getThreads(GoogleMailApiContext context, List<String> threadIds) {
        validateThreadInput(context, threadIds);

        return threadIds.stream()
                .map(threadId -> toThreadResult(getThread(context, threadId)))
                .toList();
    }

    private void validateThreadListInput(GoogleMailApiContext context) {
        if (context == null
                || isBlank(context.accessToken())
                || isBlank(context.accountKey())
                || isBlank(googleMailInitialSyncProperties.getThreadsUri())
                || googleMailInitialSyncProperties.getMaxThreads() <= 0
                || googleMailInitialSyncProperties.getThreadListPageSize() <= 0
                || googleMailInitialSyncProperties.getThreadListPageSize() > MAX_THREAD_LIST_PAGE_SIZE
                || googleMailInitialSyncProperties.getThreadBatchSize() <= 0) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }
    }

    private int remainingThreadCount(Set<String> threadIds) {
        return googleMailInitialSyncProperties.getMaxThreads() - threadIds.size();
    }

    private GoogleMailThreadListResponse requestThreadList(GoogleMailApiContext context, String pageToken, int remainingThreadCount) {
        try {
            gmailApiRateLimitService.consumeThreadList(context.accountKey());
            GoogleMailThreadListResponse response = googleMailMessageRestClient
                    .get()
                    .uri(buildThreadListUri(pageToken, remainingThreadCount))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + context.accessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleMailThreadListResponse.class);

            return validateThreadListResponse(response);
        } catch (RestClientException e) {
            String status = e instanceof RestClientResponseException re ? re.getStatusCode().toString() : "N/A";
            log.warn("Gmail thread list fetch failed. accountKey={} status={} error={}", context.accountKey(), status, e.getMessage(), e);
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }
    }

    private String buildThreadListUri(String pageToken, int remainingThreadCount) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(googleMailInitialSyncProperties.getThreadsUri())
                .queryParam("maxResults", Math.min(googleMailInitialSyncProperties.getThreadListPageSize(), remainingThreadCount));
        if (!isBlank(pageToken)) {
            builder.queryParam("pageToken", pageToken);
        }
        return builder
                .build()
                .toUriString();
    }

    private GoogleMailThreadListResponse validateThreadListResponse(GoogleMailThreadListResponse response) {
        if (response == null) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        if (response.threads() != null && response.threads().stream()
                .anyMatch(thread -> thread == null || isBlank(thread.id()))) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        return response;
    }

    private void appendThreadIds(Set<String> threadIds, GoogleMailThreadListResponse response) {
        if (response.threads() == null || response.threads().isEmpty()) {
            return;
        }

        response.threads().stream()
                .map(thread -> thread.id())
                .limit(remainingThreadCount(threadIds))
                .forEach(threadIds::add);
    }

    private GoogleMailThreadResponse getThread(GoogleMailApiContext context, String threadId) {
        try {
            gmailApiRateLimitService.consumeThreadGet(context.accountKey());
            GoogleMailThreadResponse response = googleMailMessageRestClient
                    .get()
                    .uri(buildThreadUri(threadId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + context.accessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleMailThreadResponse.class);

            return validateThreadResponse(response);
        } catch (RestClientException e) {
            String status = e instanceof RestClientResponseException re ? re.getStatusCode().toString() : "N/A";
            log.warn("Gmail thread fetch failed. threadId={} accountKey={} status={} error={}", threadId, context.accountKey(), status, e.getMessage());
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }
    }

    private String buildThreadUri(String threadId) {
        return UriComponentsBuilder.fromUriString(googleMailInitialSyncProperties.getThreadsUri())
                .pathSegment(threadId)
                .build()
                .toUriString();
    }

    private void validateThreadInput(GoogleMailApiContext context, List<String> threadIds) {
        if (context == null
                || isBlank(context.accessToken())
                || isBlank(context.accountKey())
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
        ParsedMailAddresses replyTo = extractMailAddresses(messageResponse, "Reply-To");

        return new InitialMailSyncMessageResult(
                messageResponse.id(),
                messageResponse.threadId(),
                firstNonBlank(messageResponse.historyId(), threadResponse.historyId()),
                extractHeaderValue(messageResponse, "Message-ID"),
                extractHeaderValue(messageResponse, "References"),
                extractHeaderValue(messageResponse, "In-Reply-To"),
                firstOrNull(replyTo.addresses()),
                firstOrNull(replyTo.names()),
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

    private Direction resolveDirection(List<String> labelIds) {
        return labelIds != null && labelIds.contains("SENT")
                ? Direction.OUTBOUND
                : Direction.INBOUND;
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
                    String normalizedAddress = address.getAddress().trim().toLowerCase();
                    normalizedAddresses.add(normalizedAddress);
                    names.add(normalizePersonalName(address.getPersonal(), normalizedAddress));
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
        if (isBlank(html) && !isBlank(text)) {
            html = plainTextToHtml(text);
        }
        return new MimeBodyContent(text, html);
    }

    private String plainTextToHtml(String text) {
        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        return "<div>" + HtmlUtils.htmlEscape(normalized).replace("\n", "<br>") + "</div>";
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
                        normalizeContentId(extractPartHeaderValue(part, "Content-ID")),
                        resolveDisposition(part),
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

    private String extractPartHeaderValue(
            GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload,
            String headerName
    ) {
        if (payload == null || payload.headers() == null) {
            return null;
        }

        return payload.headers().stream()
                .filter(header -> header != null
                        && !isBlank(header.name())
                        && headerName.equalsIgnoreCase(header.name()))
                .map(GoogleMailThreadResponse.GoogleMailHeaderResponse::value)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private AttachmentDisposition resolveDisposition(GoogleMailThreadResponse.GoogleMailThreadPayloadResponse part) {
        String disposition = extractPartHeaderValue(part, "Content-Disposition");
        String normalizedDisposition = disposition == null ? null : disposition.trim().toLowerCase();
        if (!isBlank(normalizedDisposition) && normalizedDisposition.startsWith("attachment")) {
            return AttachmentDisposition.ATTACHMENT;
        }

        if (!isInlineImage(part)) {
            return AttachmentDisposition.ATTACHMENT;
        }

        if (!isBlank(normalizedDisposition) && normalizedDisposition.startsWith("inline")) {
            return AttachmentDisposition.INLINE;
        }

        if (!isBlank(extractPartHeaderValue(part, "Content-ID"))) {
            return AttachmentDisposition.INLINE;
        }

        return AttachmentDisposition.ATTACHMENT;
    }

    private boolean isInlineImage(GoogleMailThreadResponse.GoogleMailThreadPayloadResponse part) {
        return part != null
                && part.mimeType() != null
                && part.mimeType().trim().toLowerCase().startsWith("image/");
    }

    private String normalizeContentId(String contentId) {
        if (isBlank(contentId)) {
            return null;
        }

        String normalizedContentId = contentId.trim();
        if (normalizedContentId.startsWith("<") && normalizedContentId.endsWith(">") && normalizedContentId.length() > 2) {
            return normalizedContentId.substring(1, normalizedContentId.length() - 1);
        }
        return normalizedContentId;
    }

    private String firstNonBlank(String primary, String secondary) {
        return !isBlank(primary) ? primary : secondary;
    }

    private String normalizePersonalName(String personalName, String address) {
        if (isBlank(personalName)) {
            return address;
        }
        return personalName.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstOrNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
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
