package com.mailsangja.worker.dto.gmail;

import java.util.List;

public record GoogleMailHistoryResponse(
        String historyId,
        List<GoogleMailHistoryItemResponse> history
) {
}
