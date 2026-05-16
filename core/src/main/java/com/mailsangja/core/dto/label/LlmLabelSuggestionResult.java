package com.mailsangja.core.dto.label;

import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.common.label.NotificationPolicy;

import java.util.List;

public record LlmLabelSuggestionResult(
        List<LabelSuggestionItem> suggestions
) {

    public LlmLabelSuggestionResult {
        suggestions = suggestions != null ? suggestions : List.of();
    }

    public record LabelSuggestionItem(
            String name,
            String colorCode,
            NotificationPolicy notificationPolicy,
            int order,
            LabelRule rule
    ) {}
}
