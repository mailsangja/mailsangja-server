package com.mailsangja.worker.dto.gmail.message;

import java.util.List;

public record GoogleMailThreadListResponse(
        List<GoogleMailThreadListItemResponse> threads,
        String nextPageToken,
        Integer resultSizeEstimate
) {
}
