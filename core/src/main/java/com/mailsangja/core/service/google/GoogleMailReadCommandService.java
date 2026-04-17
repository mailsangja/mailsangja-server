package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailReadModifyRequest;
import com.mailsangja.core.dto.mail.GoogleMailUnreadModifyRequest;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Thread;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class GoogleMailReadCommandService {

    private static final List<String> REMOVE_UNREAD_LABEL_IDS = List.of("UNREAD");
    private static final List<String> ADD_UNREAD_LABEL_IDS = List.of("UNREAD");

    private final GoogleMailProperties googleMailProperties;
    private final RestClient googleMailRestClient;

    public GoogleMailReadCommandService(
            GoogleMailProperties googleMailProperties,
            @Qualifier("googleMailRestClient") RestClient googleMailRestClient
    ) {
        this.googleMailProperties = googleMailProperties;
        this.googleMailRestClient = googleMailRestClient;
    }

    public void markThreadAsRead(MailAccount mailAccount, Thread thread) {
        validateInput(mailAccount, thread, MailAccountErrorCode.GOOGLE_MAIL_READ_MODIFY_FAILED);

        GoogleMailReadModifyRequest request = new GoogleMailReadModifyRequest(REMOVE_UNREAD_LABEL_IDS);

        try {
            googleMailRestClient
                    .post()
                    .uri(
                            googleMailProperties.getThreadModifyUri(),
                            Map.of("gmailThreadId", thread.getGmailThreadId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mailAccount.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_READ_MODIFY_FAILED);
        }
    }

    public void markMessageAsRead(MailAccount mailAccount, Message message) {
        validateInput(mailAccount, message, MailAccountErrorCode.GOOGLE_MESSAGE_READ_MODIFY_FAILED);

        GoogleMailReadModifyRequest request = new GoogleMailReadModifyRequest(REMOVE_UNREAD_LABEL_IDS);

        try {
            googleMailRestClient
                    .post()
                    .uri(
                            googleMailProperties.getMessageModifyUri(),
                            Map.of("gmailMessageId", message.getGmailMessageId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mailAccount.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MESSAGE_READ_MODIFY_FAILED);
        }
    }

    public void markMessageAsUnread(MailAccount mailAccount, Message message) {
        validateInput(mailAccount, message, MailAccountErrorCode.GOOGLE_MESSAGE_UNREAD_MODIFY_FAILED);

        GoogleMailUnreadModifyRequest request = new GoogleMailUnreadModifyRequest(ADD_UNREAD_LABEL_IDS);

        try {
            googleMailRestClient
                    .post()
                    .uri(
                            googleMailProperties.getMessageModifyUri(),
                            Map.of("gmailMessageId", message.getGmailMessageId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mailAccount.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MESSAGE_UNREAD_MODIFY_FAILED);
        }
    }

    public void markThreadAsUnread(MailAccount mailAccount, Thread thread) {
        validateInput(mailAccount, thread, MailAccountErrorCode.GOOGLE_MAIL_UNREAD_MODIFY_FAILED);

        GoogleMailUnreadModifyRequest request = new GoogleMailUnreadModifyRequest(ADD_UNREAD_LABEL_IDS);

        try {
            googleMailRestClient
                    .post()
                    .uri(
                            googleMailProperties.getThreadModifyUri(),
                            Map.of("gmailThreadId", thread.getGmailThreadId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mailAccount.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_UNREAD_MODIFY_FAILED);
        }
    }

    private void validateInput(MailAccount mailAccount, Thread thread, MailAccountErrorCode errorCode) {
        if (mailAccount == null
                || thread == null
                || mailAccount.getProvider() != MailProvider.GMAIL
                || isBlank(mailAccount.getAccessToken())
                || isBlank(thread.getGmailThreadId())
                || isBlank(googleMailProperties.getThreadModifyUri())) {
            throw new MailAccountException(errorCode);
        }
    }

    private void validateInput(MailAccount mailAccount, Message message, MailAccountErrorCode errorCode) {
        if (mailAccount == null
                || message == null
                || mailAccount.getProvider() != MailProvider.GMAIL
                || isBlank(mailAccount.getAccessToken())
                || isBlank(message.getGmailMessageId())
                || isBlank(googleMailProperties.getMessageModifyUri())) {
            throw new MailAccountException(errorCode);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
