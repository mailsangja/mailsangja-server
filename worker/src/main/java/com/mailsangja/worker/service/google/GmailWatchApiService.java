package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailWatchProperties;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchRequest;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResponse;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class GmailWatchApiService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final GoogleMailWatchProperties googleMailWatchProperties;
    private final RestClient googleMailWatchRestClient;

    public GmailWatchApiService(
            GoogleMailWatchProperties googleMailWatchProperties,
            @Qualifier("googleMailWatchRestClient") RestClient googleMailWatchRestClient
    ) {
        this.googleMailWatchProperties = googleMailWatchProperties;
        this.googleMailWatchRestClient = googleMailWatchRestClient;
    }

    public GoogleMailWatchResult watch(String accessToken) {
        validateWatchInput(accessToken);

        GoogleMailWatchRequest request = new GoogleMailWatchRequest(
                googleMailWatchProperties.getTopicName(),
                normalizeLabelIds(googleMailWatchProperties.getLabelIds()),
                normalizeBlankToNull(googleMailWatchProperties.getLabelFilterBehavior())
        );

        log.info(
                "Gmail watch request started. topicName={} labelIds={} labelFilterBehavior={}",
                request.topicName(),
                request.labelIds(),
                request.labelFilterBehavior()
        );

        try {
            GoogleMailWatchResponse response = googleMailWatchRestClient
                    .post()
                    .uri(googleMailWatchProperties.getWatchUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GoogleMailWatchResponse.class);

            GoogleMailWatchResult result = validateWatchResponse(response);
            log.info(
                    "Gmail watch request completed. historyId={} expirationAt={}",
                    result.historyId(),
                    result.expirationAt()
            );
            return result;
        } catch (RestClientException e) {
            log.warn(
                    "Gmail watch request failed. topicName={} labelIds={} labelFilterBehavior={} status={} error={}",
                    request.topicName(),
                    request.labelIds(),
                    request.labelFilterBehavior(),
                    status(e),
                    e.getMessage(),
                    e
            );
            throw new MailPushException(MailPushErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }
    }

    private String status(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().toString();
        }
        return "N/A";
    }

    private void validateWatchInput(String accessToken) {
        if (isBlank(accessToken)
                || isBlank(googleMailWatchProperties.getTopicName())
                || isBlank(googleMailWatchProperties.getWatchUri())) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }

        List<String> labelIds = googleMailWatchProperties.getLabelIds();
        if (labelIds != null && labelIds.stream().anyMatch(this::isBlank)) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }
    }

    private GoogleMailWatchResult validateWatchResponse(GoogleMailWatchResponse response) {
        if (response == null || isBlank(response.historyId()) || isBlank(response.expiration())) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_MAIL_WATCH_RESULT_INVALID);
        }

        try {
            LocalDateTime expirationAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(response.expiration())),
                    KST_ZONE_ID
            );

            return new GoogleMailWatchResult(response.historyId(), expirationAt);
        } catch (NumberFormatException e) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_MAIL_WATCH_RESULT_INVALID);
        }
    }

    private List<String> normalizeLabelIds(List<String> labelIds) {
        if (labelIds == null) {
            return null;
        }

        List<String> normalizedLabelIds = labelIds.stream()
                .map(this::normalizeBlankToNull)
                .filter(labelId -> labelId != null)
                .toList();

        return normalizedLabelIds.isEmpty() ? null : normalizedLabelIds;
    }

    private String normalizeBlankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
