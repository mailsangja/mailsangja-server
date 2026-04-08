package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleMailWatchProperties;
import com.mailsangja.core.dto.mail.GoogleMailWatchRequest;
import com.mailsangja.core.dto.mail.GoogleMailWatchResponse;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Service
public class GoogleMailWatchQueryService {

    private final GoogleMailWatchProperties googleMailWatchProperties;
    private final RestClient googleMailWatchRestClient;

    public GoogleMailWatchQueryService(
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

            return validateWatchResponse(response);
        } catch (RestClientResponseException e) {
            log.warn(
                    "Google Gmail watch request failed with status={} and response={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        } catch (RestClientException e) {
            log.warn("Google Gmail watch request failed: {}", e.getMessage());
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }
    }

    private void validateWatchInput(String accessToken) {
        if (isBlank(accessToken)
                || isBlank(googleMailWatchProperties.getTopicName())
                || isBlank(googleMailWatchProperties.getWatchUri())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }

        List<String> labelIds = googleMailWatchProperties.getLabelIds();
        if (labelIds != null && labelIds.stream().anyMatch(this::isBlank)) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_FAILED);
        }
    }

    private GoogleMailWatchResult validateWatchResponse(GoogleMailWatchResponse response) {
        if (response == null || isBlank(response.historyId())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_WATCH_RESULT_INVALID);
        }

        return new GoogleMailWatchResult(response.historyId());
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
