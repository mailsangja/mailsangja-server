package com.mailsangja.worker.dto.gmail.history;

import java.util.List;

public record GoogleMailHistoryResponse(
        String historyId,
        List<GoogleMailHistoryItemResponse> history
) {
}
