package com.mailsangja.worker.dto.gmail.history;

import java.util.List;

public record GoogleMailHistoryItemResponse(
        String id,
        List<GoogleMailHistoryLabelChangeResponse> labelsAdded,
        List<GoogleMailHistoryLabelChangeResponse> labelsRemoved,
        List<GoogleMailHistoryLabelChangeResponse> messagesDeleted
) {
}
