package com.mailsangja.worker.dto.mail.sync;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;

import java.util.List;
import java.util.UUID;

public record InitialMailSyncThreadBatchMessage(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress,
        List<String> threadIds
) {
    public InitialMailSyncThreadBatchMessage {
        if (mailAccountId == null || userId == null
                || provider == null || provider.isBlank()
                || emailAddress == null || emailAddress.isBlank()
                || threadIds == null || threadIds.isEmpty()
                || threadIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }
    }
}
