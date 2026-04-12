package com.mailsangja.worker.dto.gmail;

import java.util.List;

public record GoogleMailHistoryListResult(
        String historyId,
        List<GoogleMailHistoryItemResult> histories
) {
}
