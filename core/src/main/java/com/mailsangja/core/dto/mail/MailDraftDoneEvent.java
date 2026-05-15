package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

public record MailDraftDoneEvent(String status) {

    public MailDraftDoneEvent {
        if (status == null || status.isBlank()) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    public static MailDraftDoneEvent success() {
        return new MailDraftDoneEvent("done");
    }
}
