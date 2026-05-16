package com.mailsangja.core.dto.mail;

public record LlmMailReviewSegment(
        String segmentId,
        MailReviewField field,
        String text
) {

    public static LlmMailReviewSegment from(MailReviewSegment segment) {
        return new LlmMailReviewSegment(segment.segmentId(), segment.field(), segment.text());
    }
}
