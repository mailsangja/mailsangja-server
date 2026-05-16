package com.mailsangja.core.dto.mail;

import java.util.List;

public record MailReviewResponse(
        boolean hasIssues,
        List<MailReviewIssueResponse> issues
) {

    public static MailReviewResponse from(MailReviewResult result) {
        List<MailReviewIssueResponse> issues = result.issues().stream()
                .map(MailReviewIssueResponse::from)
                .toList();
        return new MailReviewResponse(!issues.isEmpty(), issues);
    }
}
