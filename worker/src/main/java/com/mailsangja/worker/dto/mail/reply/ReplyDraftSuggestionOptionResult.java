package com.mailsangja.worker.dto.mail.reply;

import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;

public record ReplyDraftSuggestionOptionResult(
        String type,
        String subject,
        String body
) {

    public ReplyDraftSuggestionOptionResult {
        if (isBlank(type) || isBlank(subject) || isBlank(body)) {
            throw new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_AI_RESPONSE);
        }
        type = type.trim();
        subject = subject.trim();
        body = body.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
