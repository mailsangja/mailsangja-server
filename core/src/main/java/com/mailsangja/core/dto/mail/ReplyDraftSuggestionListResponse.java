package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;

import java.util.List;

public record ReplyDraftSuggestionListResponse(
        List<ReplyDraftSuggestionResponse> suggestions
) {

    public ReplyDraftSuggestionListResponse {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    public static ReplyDraftSuggestionListResponse from(List<ReplyDraftSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return new ReplyDraftSuggestionListResponse(List.of());
        }
        return new ReplyDraftSuggestionListResponse(
                suggestions.stream()
                        .map(ReplyDraftSuggestionResponse::from)
                        .toList()
        );
    }
}
