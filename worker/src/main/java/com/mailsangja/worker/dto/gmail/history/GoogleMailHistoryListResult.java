package com.mailsangja.worker.dto.gmail.history;

import java.util.List;

public record GoogleMailHistoryListResult(
        String historyId,
        List<GoogleMailHistoryItemResult> histories
) {
}
