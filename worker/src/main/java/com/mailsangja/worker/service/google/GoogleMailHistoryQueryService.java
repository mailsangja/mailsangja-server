package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailHistoryProperties;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryItemResponse;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryItemResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryLabelChangeResponse;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryLabelChangeResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryListResult;
import com.mailsangja.worker.dto.gmail.history.GoogleMailHistoryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class GoogleMailHistoryQueryService {

    private final GoogleMailHistoryProperties googleMailHistoryProperties;
    private final RestClient googleMailHistoryRestClient;

    public GoogleMailHistoryQueryService(
            GoogleMailHistoryProperties googleMailHistoryProperties,
            @Qualifier("googleMailHistoryRestClient") RestClient googleMailHistoryRestClient
    ) {
        this.googleMailHistoryProperties = googleMailHistoryProperties;
        this.googleMailHistoryRestClient = googleMailHistoryRestClient;
    }

    public GoogleMailHistoryListResult getHistory(String accessToken, String startHistoryId) {
        validateHistoryInput(accessToken, startHistoryId);

        try {
            GoogleMailHistoryResponse response = googleMailHistoryRestClient
                    .get()
                    .uri(buildHistoryUri(startHistoryId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleMailHistoryResponse.class);

            return validateHistoryResponse(response);
        } catch (RestClientException e) {
            throw new MailPushException(MailPushErrorCode.GMAIL_HISTORY_FETCH_FAILED);
        }
    }

    private void validateHistoryInput(String accessToken, String startHistoryId) {
        if (isBlank(accessToken)
                || isBlank(startHistoryId)
                || isBlank(googleMailHistoryProperties.getHistoryUri())) {
            throw new MailPushException(MailPushErrorCode.GMAIL_HISTORY_FETCH_FAILED);
        }
    }

    private String buildHistoryUri(String startHistoryId) {
        return UriComponentsBuilder.fromUriString(googleMailHistoryProperties.getHistoryUri())
                .queryParam("startHistoryId", startHistoryId)
                .build()
                .toUriString();
    }

    private GoogleMailHistoryListResult validateHistoryResponse(GoogleMailHistoryResponse response) {
        if (response == null || isBlank(response.historyId())) {
            throw new MailPushException(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID);
        }

        return new GoogleMailHistoryListResult(
                response.historyId(),
                mapHistoryItems(response.history())
        );
    }

    private List<GoogleMailHistoryItemResult> mapHistoryItems(List<GoogleMailHistoryItemResponse> historyItems) {
        if (historyItems == null || historyItems.isEmpty()) {
            return List.of();
        }

        return historyItems.stream()
                .map(this::mapHistoryItem)
                .toList();
    }

    private GoogleMailHistoryItemResult mapHistoryItem(GoogleMailHistoryItemResponse historyItem) {
        if (historyItem == null) {
            return new GoogleMailHistoryItemResult(null, List.of(), List.of(), List.of());
        }

        return new GoogleMailHistoryItemResult(
                historyItem.id(),
                mapLabelChanges(historyItem.labelsAdded()),
                mapLabelChanges(historyItem.labelsRemoved()),
                mapLabelChanges(historyItem.messagesDeleted())
        );
    }

    private List<GoogleMailHistoryLabelChangeResult> mapLabelChanges(List<GoogleMailHistoryLabelChangeResponse> labelChanges) {
        if (labelChanges == null || labelChanges.isEmpty()) {
            return List.of();
        }

        return labelChanges.stream()
                .map(this::mapLabelChange)
                .toList();
    }

    private GoogleMailHistoryLabelChangeResult mapLabelChange(GoogleMailHistoryLabelChangeResponse labelChange) {
        if (labelChange == null || labelChange.message() == null) {
            return new GoogleMailHistoryLabelChangeResult(null, null, List.of());
        }

        return new GoogleMailHistoryLabelChangeResult(
                labelChange.message().id(),
                labelChange.message().threadId(),
                labelChange.labelIds() == null ? List.of() : List.copyOf(labelChange.labelIds())
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
