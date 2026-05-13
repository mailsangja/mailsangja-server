package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.dto.mail.MailDraftPhase;

public record MailDraftDeltaEvent(MailDraftPhase phase, String delta) {
}
