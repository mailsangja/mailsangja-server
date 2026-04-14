package com.mailsangja.worker.dto.gmail.push;

public record GoogleMailPushNotificationResult(
        String emailAddress,
        String historyId
) {
}
