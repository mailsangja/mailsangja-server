package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailWatchRequest;
import com.mailsangja.core.dto.mail.GoogleMailWatchResponse;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class GoogleMailWatchQueryService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final GoogleMailProperties googleMailProperties;
    private final RestClient googleMailRestClient;

    public GoogleMailWatchQueryService(
            GoogleMailProperties googleMailProperties,
            @Qualifier("googleMailRestClient") RestClient googleMailRestClient
    ) {
        this.googleMailProperties = googleMailProperties;
        this.googleMailRestClient = googleMailRestClient;
    }

    public GoogleMailWatchResult watch(String accessToken) {
        validateWatchInput(accessToken);

        GoogleMailWatchRequest request = new GoogleMailWatchRequest(
                googleMailProperties.getTopicName(),
                normalizeLabelIds(googleMailProperties.getLabelIds()),
                normalizeBlankToNull(googleMailProperties.getLabelFilterBehavior())
        );

        try {
            GoogleMailWatchResponse response = googleMailRestClient
                    .post()
                    .uri(googleMailProperties.getWatchUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GoogleMailWatchResponse.class);

            return validateWatchResponse(response);
        } catch (RestClientException e) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }
    }

    private void validateWatchInput(String accessToken) {
        if (isBlank(accessToken)
                || isBlank(googleMailProperties.getTopicName())
                || isBlank(googleMailProperties.getWatchUri())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }

        List<String> labelIds = googleMailProperties.getLabelIds();
        if (labelIds != null && labelIds.stream().anyMatch(this::isBlank)) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }
    }

    private GoogleMailWatchResult validateWatchResponse(GoogleMailWatchResponse response) {
        if (response == null || isBlank(response.historyId()) || isBlank(response.expiration())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_RESULT_INVALID);
        }

        try {
            LocalDateTime expirationAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(response.expiration())),
                    KST_ZONE_ID
            );

            return new GoogleMailWatchResult(response.historyId(), expirationAt);
        } catch (NumberFormatException e) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_RESULT_INVALID);
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
