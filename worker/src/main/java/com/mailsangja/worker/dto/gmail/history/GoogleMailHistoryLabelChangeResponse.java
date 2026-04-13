package com.mailsangja.worker.dto.gmail.history;

import java.util.List;

public record GoogleMailHistoryLabelChangeResponse(
        GoogleMailHistoryMessageRefResponse message,
        List<String> labelIds
) {
}
