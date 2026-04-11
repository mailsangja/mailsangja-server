package com.mailsangja.worker.dto.gmail;

import java.util.Map;

public record GooglePubsubMessageRequest(
        String data,
        String messageId,
        String publishTime,
        Map<String, String> attributes
) {
}
