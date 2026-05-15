package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

public record MailDraftDeltaEvent(MailDraftPhase phase, String delta) {

    public MailDraftDeltaEvent {
        validatePhase(phase);
        validateDelta(delta);
    }

    private static void validatePhase(MailDraftPhase phase) {
        if (phase == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateDelta(String delta) {
        if (delta == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }
}
