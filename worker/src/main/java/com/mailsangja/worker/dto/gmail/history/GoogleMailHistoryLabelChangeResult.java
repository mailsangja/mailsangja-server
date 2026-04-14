package com.mailsangja.worker.dto.gmail.history;

import java.util.List;

public record GoogleMailHistoryLabelChangeResult(
        String gmailMessageId,
        String gmailThreadId,
        List<String> labelIds
) {
}
