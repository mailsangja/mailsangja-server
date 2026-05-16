package com.mailsangja.core.dto.mail;

public record MailReviewIssueResponse(
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

    public static MailReviewIssueResponse from(MailReviewIssueResult issue) {
        return new MailReviewIssueResponse(
                issue.segmentId(),
                issue.field(),
                issue.type(),
                issue.severity(),
                issue.segmentText(),
                issue.originalText(),
                issue.replacementText(),
                issue.localStartOffset(),
                issue.localEndOffset(),
                issue.globalStartOffset(),
                issue.globalEndOffset(),
                issue.reason()
        );
    }
}
