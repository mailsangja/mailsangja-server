package com.mailsangja.worker.dto.mail.reply;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReplyDraftSuggestionContextResult(
        UUID messageId,
        String source,
        Direction direction,
        LocalDateTime sentAt,
        String from,
        List<String> to,
        List<String> cc,
        String subject,
        String body
) {

    public ReplyDraftSuggestionContextResult {
        if (messageId == null) {
            throw new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE);
        }
        source = nullToEmpty(source);
        from = nullToEmpty(from);
        to = nullToEmpty(to);
        cc = nullToEmpty(cc);
        subject = nullToEmpty(subject);
        body = nullToEmpty(body);
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private static List<String> nullToEmpty(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }
}
