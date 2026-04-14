package com.mailsangja.worker.dto.gmail.push;

public record GooglePubsubPushRequest(
        GooglePubsubMessageRequest message,
        String subscription
) {
}
