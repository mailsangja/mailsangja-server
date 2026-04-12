package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.GoogleMailWatchProperties;
import com.mailsangja.core.dto.mail.GoogleMailReadModifyRequest;
import com.mailsangja.db.entity.mail.MailAccount;
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

    private final GoogleMailWatchProperties googleMailWatchProperties;
    private final RestClient googleMailRestClient;

    public GoogleMailReadCommandService(
            GoogleMailWatchProperties googleMailWatchProperties,
            @Qualifier("googleMailRestClient") RestClient googleMailRestClient
    ) {
        this.googleMailWatchProperties = googleMailWatchProperties;
        this.googleMailRestClient = googleMailRestClient;
    }

    public void markThreadAsRead(MailAccount mailAccount, Thread thread) {
        validateInput(mailAccount, thread);

        GoogleMailReadModifyRequest request = new GoogleMailReadModifyRequest(REMOVE_UNREAD_LABEL_IDS);

        try {
            googleMailRestClient
                    .post()
                    .uri(
                            googleMailWatchProperties.getThreadModifyUri(),
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

    private void validateInput(MailAccount mailAccount, Thread thread) {
        if (mailAccount == null
                || thread == null
                || mailAccount.getProvider() != MailProvider.GMAIL
                || isBlank(mailAccount.getAccessToken())
                || isBlank(thread.getGmailThreadId())
                || isBlank(googleMailWatchProperties.getThreadModifyUri())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_MAIL_READ_MODIFY_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
