package com.mailsangja.worker.dto.mail.sync;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;

import java.util.UUID;

public record InitialMailSyncMessage(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress
) {
    public InitialMailSyncMessage {
        if (mailAccountId == null || userId == null
                || provider == null || provider.isBlank()
                || emailAddress == null || emailAddress.isBlank()) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }
    }
}
