package com.mailsangja.core.dto.mail;

import java.util.List;

public record LlmMailReviewResult(
        List<LlmMailReviewIssueResult> issues
) {

    public LlmMailReviewResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
