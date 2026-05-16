package com.mailsangja.core.dto.mail;

public record MailReviewIssueResult(
        String segmentId,
        MailReviewField field,
        MailReviewIssueType type,
        MailReviewSeverity severity,
        String segmentText,
        String originalText,
        String replacementText,
        int localStartOffset,
        int localEndOffset,
        int globalStartOffset,
        int globalEndOffset,
        String reason
) {
}
