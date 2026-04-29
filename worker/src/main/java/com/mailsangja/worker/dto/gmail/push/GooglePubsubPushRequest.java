package com.mailsangja.worker.dto.gmail.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;

import java.io.IOException;
import java.util.Base64;

public record GooglePubsubPushRequest(
        GooglePubsubMessageRequest message,
        String subscription
) {
    public GoogleMailPushNotificationResult decode(ObjectMapper objectMapper) {
        if (message == null || message.data() == null || message.data().isBlank()) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_MESSAGE_DATA);
        }
        try {
            byte[] decodedData = Base64.getDecoder().decode(message.data());
            return objectMapper.readValue(decodedData, GoogleMailPushNotificationResult.class);
        } catch (IllegalArgumentException | IOException e) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_MESSAGE_DATA);
        }
    }
}
