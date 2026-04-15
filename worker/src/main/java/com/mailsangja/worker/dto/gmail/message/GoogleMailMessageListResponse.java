package com.mailsangja.worker.dto.gmail.message;

import java.util.List;

public record GoogleMailMessageListResponse(
        List<GoogleMailMessageResponse> messages,
        Integer resultSizeEstimate
) {
}
