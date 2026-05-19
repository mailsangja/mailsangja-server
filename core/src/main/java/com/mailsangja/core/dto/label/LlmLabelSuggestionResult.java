package com.mailsangja.core.dto.label;

import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.common.label.NotificationPolicy;

import java.util.List;
import java.util.Objects;

public record LlmLabelSuggestionResult(
        List<LabelSuggestionItem> suggestions
) {

    public LlmLabelSuggestionResult {
        suggestions = suggestions != null
                ? suggestions.stream().filter(Objects::nonNull).toList()
                : List.of();
    }

    public record LabelSuggestionItem(
            String name,
            String colorCode,
            NotificationPolicy notificationPolicy,
            int order,
            LabelRule rule
    ) {}
}
