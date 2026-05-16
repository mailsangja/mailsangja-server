package com.mailsangja.core.dto.mail;

import java.util.List;

public record LlmMailReviewRequest(
        LlmMailReviewMetadata metadata,
        List<LlmMailReviewSegment> segments
) {

    public LlmMailReviewRequest {
        metadata = metadata == null ? new LlmMailReviewMetadata(0, List.of()) : metadata;
        segments = segments == null ? List.of() : List.copyOf(segments);
    }

    public static LlmMailReviewRequest of(MailReviewCommand command, List<MailReviewSegment> segments) {
        return new LlmMailReviewRequest(LlmMailReviewMetadata.from(command), segments.stream()
                .map(LlmMailReviewSegment::from)
                .toList());
    }
}
