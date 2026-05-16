package com.mailsangja.core.dto.mail;

import java.util.List;
import java.util.Objects;

public record LlmMailReviewResult(
        List<LlmMailReviewIssueResult> issues
) {

    public LlmMailReviewResult {
        issues = issues == null ? List.of() : issues.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
