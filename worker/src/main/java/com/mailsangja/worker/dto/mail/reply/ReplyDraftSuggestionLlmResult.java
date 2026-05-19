package com.mailsangja.worker.dto.mail.reply;

import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;

import java.util.List;

public record ReplyDraftSuggestionLlmResult(
        List<ReplyDraftSuggestionOptionResult> suggestions
) {

    public ReplyDraftSuggestionLlmResult {
        if (suggestions == null || suggestions.size() < 2 || suggestions.size() > 3 || hasNullSuggestion(suggestions)) {
            throw new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_AI_RESPONSE);
        }
        suggestions = List.copyOf(suggestions);
    }

    private static boolean hasNullSuggestion(List<ReplyDraftSuggestionOptionResult> suggestions) {
        for (ReplyDraftSuggestionOptionResult suggestion : suggestions) {
            if (suggestion == null) {
                return true;
            }
        }
        return false;
    }
}
