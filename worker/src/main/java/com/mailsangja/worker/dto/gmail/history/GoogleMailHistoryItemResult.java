package com.mailsangja.worker.dto.gmail.history;

import java.util.List;

public record GoogleMailHistoryItemResult(
        String historyId,
        List<GoogleMailHistoryLabelChangeResult> labelsAdded,
        List<GoogleMailHistoryLabelChangeResult> labelsRemoved,
        List<GoogleMailHistoryLabelChangeResult> messagesDeleted,
        List<GoogleMailHistoryMessageAddedResult> messagesAdded
) {
}
