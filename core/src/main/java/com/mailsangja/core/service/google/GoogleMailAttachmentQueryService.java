package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.config.properties.GoogleMailProperties;
import com.mailsangja.core.dto.mail.GoogleMailAttachmentResponse;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;

@Service
public class GoogleMailAttachmentQueryService {

    private final GoogleMailProperties googleMailProperties;
    private final RestClient googleMailRestClient;

    public GoogleMailAttachmentQueryService(
            GoogleMailProperties googleMailProperties,
            @Qualifier("googleMailRestClient") RestClient googleMailRestClient
    ) {
        this.googleMailProperties = googleMailProperties;
        this.googleMailRestClient = googleMailRestClient;
    }

    public byte[] download(MailAccount mailAccount, Message message, Attachment attachment) {
        validateInput(mailAccount, message, attachment);

        try {
            GoogleMailAttachmentResponse response = googleMailRestClient
                    .get()
                    .uri(buildAttachmentUri(message.getGmailMessageId(), attachment.getGmailAttachmentId()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mailAccount.getAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleMailAttachmentResponse.class);

            return decodeAttachmentData(validateResponse(response));
        } catch (RestClientException e) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_DOWNLOAD_FAILED);
        }
    }

    private void validateInput(MailAccount mailAccount, Message message, Attachment attachment) {
        if (mailAccount == null
                || message == null
                || attachment == null
                || isBlank(mailAccount.getAccessToken())
                || isBlank(message.getGmailMessageId())
                || isBlank(attachment.getGmailAttachmentId())
                || isBlank(googleMailProperties.getAttachmentsUri())) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_SOURCE_INVALID);
        }
    }

    private String buildAttachmentUri(String gmailMessageId, String gmailAttachmentId) {
        return UriComponentsBuilder.fromUriString(googleMailProperties.getAttachmentsUri())
                .pathSegment(gmailMessageId, "attachments", gmailAttachmentId)
                .build()
                .toUriString();
    }

    private GoogleMailAttachmentResponse validateResponse(GoogleMailAttachmentResponse response) {
        if (response == null) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_SOURCE_INVALID);
        }

        return response;
    }

    private byte[] decodeAttachmentData(GoogleMailAttachmentResponse response) {
        if (isBlank(response.data())) {
            if (response.size() != null && response.size() == 0) {
                return new byte[0];
            }
            throw new InboxException(InboxErrorCode.ATTACHMENT_SOURCE_INVALID);
        }

        try {
            return Base64.getUrlDecoder().decode(response.data());
        } catch (IllegalArgumentException e) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_SOURCE_INVALID);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
