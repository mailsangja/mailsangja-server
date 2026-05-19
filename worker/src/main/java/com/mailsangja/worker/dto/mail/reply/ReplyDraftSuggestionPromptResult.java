package com.mailsangja.worker.dto.mail.reply;

import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;

public record ReplyDraftSuggestionPromptResult(
        String systemPrompt,
        String userPrompt
) {

    public ReplyDraftSuggestionPromptResult {
        if (isBlank(systemPrompt) || isBlank(userPrompt)) {
            throw new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
