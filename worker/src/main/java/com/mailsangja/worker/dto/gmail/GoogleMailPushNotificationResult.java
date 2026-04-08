package com.mailsangja.worker.dto.gmail;

public record GoogleMailPushNotificationResult(
        String emailAddress,
        String historyId
) {
}
