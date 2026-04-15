package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.trash.TrashErrorCode;
import com.mailsangja.core.common.exception.trash.TrashException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GoogleGmailApiService {

    private final RestClient googleGmailApiRestClient;

    public GoogleGmailApiService(@Qualifier("googleGmailApiRestClient") RestClient googleGmailApiRestClient) {
        this.googleGmailApiRestClient = googleGmailApiRestClient;
    }

    public void trashThread(String accessToken, String gmailThreadId) {
        try {
            googleGmailApiRestClient
                    .post()
                    .uri("/gmail/v1/users/me/threads/{id}/trash", gmailThreadId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new TrashException(TrashErrorCode.GMAIL_API_FAILED);
        }
    }

    public void untrashThread(String accessToken, String gmailThreadId) {
        try {
            googleGmailApiRestClient
                    .post()
                    .uri("/gmail/v1/users/me/threads/{id}/untrash", gmailThreadId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new TrashException(TrashErrorCode.GMAIL_API_FAILED);
        }
    }

    public void trashMessage(String accessToken, String gmailMessageId) {
        try {
            googleGmailApiRestClient
                    .post()
                    .uri("/gmail/v1/users/me/messages/{id}/trash", gmailMessageId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new TrashException(TrashErrorCode.GMAIL_API_FAILED);
        }
    }

    public void untrashMessage(String accessToken, String gmailMessageId) {
        try {
            googleGmailApiRestClient
                    .post()
                    .uri("/gmail/v1/users/me/messages/{id}/untrash", gmailMessageId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new TrashException(TrashErrorCode.GMAIL_API_FAILED);
        }
    }
}
