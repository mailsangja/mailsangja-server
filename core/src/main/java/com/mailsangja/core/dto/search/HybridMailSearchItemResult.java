package com.mailsangja.core.dto.search;

import com.mailsangja.db.entity.mail.Message;

import java.util.List;

public record HybridMailSearchItemResult(
        Message message,
        List<HybridMailSearchMatchType> matchedBy,
        double score
) {
    public HybridMailSearchItemResult {
        matchedBy = matchedBy == null ? List.of() : List.copyOf(matchedBy);
    }
}
