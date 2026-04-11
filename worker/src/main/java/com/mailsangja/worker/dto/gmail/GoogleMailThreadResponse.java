package com.mailsangja.worker.dto.gmail;

import java.util.List;

public record GoogleMailThreadResponse(
        String id,
        String historyId,
        List<GoogleMailThreadMessageResponse> messages
) {

    public record GoogleMailThreadMessageResponse(
            String id,
            String threadId,
            List<String> labelIds,
            String snippet,
            String historyId,
            String internalDate,
            GoogleMailThreadPayloadResponse payload
    ) {
    }

    public record GoogleMailThreadPayloadResponse(
            List<GoogleMailHeaderResponse> headers
    ) {
    }

    public record GoogleMailHeaderResponse(
            String name,
            String value
    ) {
    }
}
