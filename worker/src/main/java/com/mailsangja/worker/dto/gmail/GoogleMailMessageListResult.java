package com.mailsangja.worker.dto.gmail;

public record GoogleMailMessageListResult(
        int fetchedCount,
        int resultSizeEstimate
) {
}
