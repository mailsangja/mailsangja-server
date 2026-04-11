package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.gmail.GoogleMailMessageListResponse;
import com.mailsangja.worker.dto.gmail.GoogleMailMessageListResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleMailMessageQueryService {

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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
