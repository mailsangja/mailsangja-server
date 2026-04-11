package com.mailsangja.worker.dto.gmail;

public record GooglePubsubPushRequest(
        GooglePubsubMessageRequest message,
        String subscription
) {
}
