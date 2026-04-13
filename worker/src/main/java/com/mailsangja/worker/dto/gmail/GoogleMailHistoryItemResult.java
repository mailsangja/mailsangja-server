package com.mailsangja.worker.dto.gmail;

import java.util.List;

public record GoogleMailHistoryItemResult(
        String historyId,
        List<GoogleMailHistoryLabelChangeResult> labelsAdded,
        List<GoogleMailHistoryLabelChangeResult> labelsRemoved
) {
}
