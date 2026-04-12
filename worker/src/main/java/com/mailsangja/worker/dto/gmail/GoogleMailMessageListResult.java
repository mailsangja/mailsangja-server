package com.mailsangja.worker.dto.gmail;

import java.util.List;

public record GoogleMailMessageListResult(
        List<GoogleMailMessageResponse> messages,
        int resultSizeEstimate
) {
    public int fetchedCount() {
        return messages == null ? 0 : messages.size();
    }
}
