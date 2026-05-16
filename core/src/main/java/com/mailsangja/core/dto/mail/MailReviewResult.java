package com.mailsangja.core.dto.mail;

import java.util.List;

public record MailReviewResult(
        List<MailReviewIssueResult> issues
) {

    public MailReviewResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
