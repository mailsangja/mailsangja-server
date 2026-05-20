package com.mailsangja.worker.dto.mail.reply;

import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;

import java.util.UUID;

public record ReplyDraftSuggestionMessage(UUID messageId) {

    public ReplyDraftSuggestionMessage {
        if (messageId == null) {
            throw new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE);
        }
    }
}
