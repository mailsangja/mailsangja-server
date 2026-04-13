package com.mailsangja.worker.dto.gmail;

import java.util.List;

public record GoogleMailHistoryLabelChangeResult(
        String gmailMessageId,
        String gmailThreadId,
        List<String> labelIds
) {
}
