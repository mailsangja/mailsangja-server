package com.mailsangja.worker.dto.gmail.history;

import java.util.List;

public record GoogleMailHistoryMessageRefResponse(
        String id,
        String threadId,
        List<String> labelIds
) {
}
