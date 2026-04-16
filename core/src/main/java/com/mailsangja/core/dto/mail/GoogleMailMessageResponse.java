package com.mailsangja.core.dto.mail;

import java.util.List;

public record GoogleMailMessageResponse(
        String id,
        String threadId,
        List<String> labelIds,
        String snippet,
        String historyId,
        String internalDate,
        GoogleMailPayloadResponse payload
) {

    public record GoogleMailPayloadResponse(
            String mimeType,
            String filename,
            GoogleMailBodyResponse body,
            List<GoogleMailHeaderResponse> headers,
            List<GoogleMailPayloadResponse> parts
    ) {
    }

    public record GoogleMailBodyResponse(
            Integer size,
            String data,
            String attachmentId
    ) {
    }

    public record GoogleMailHeaderResponse(
            String name,
            String value
    ) {
    }
}
