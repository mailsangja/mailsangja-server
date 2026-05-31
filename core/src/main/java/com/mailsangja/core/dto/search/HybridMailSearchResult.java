package com.mailsangja.core.dto.search;

import java.util.List;
import java.util.Map;

public record HybridMailSearchResult(
        List<HybridMailSearchItemResult> items,
        Map<String, String> contactNameByEmail
) {
    public HybridMailSearchResult {
        items = items == null ? List.of() : List.copyOf(items);
        contactNameByEmail = contactNameByEmail == null ? Map.of() : Map.copyOf(contactNameByEmail);
    }
}
