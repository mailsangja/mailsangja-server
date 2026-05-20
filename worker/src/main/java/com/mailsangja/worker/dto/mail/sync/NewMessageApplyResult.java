package com.mailsangja.worker.dto.mail.sync;

import java.util.UUID;

public record NewMessageApplyResult(UUID messageId, UUID threadId, int threadMessageCount) {
}
