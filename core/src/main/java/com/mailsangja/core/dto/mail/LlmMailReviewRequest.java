package com.mailsangja.core.dto.mail;

import java.util.List;

public record LlmMailReviewRequest(
        List<LlmMailReviewSegment> segments
) {

    public LlmMailReviewRequest {
        segments = segments == null ? List.of() : List.copyOf(segments);
    }

    public static LlmMailReviewRequest from(List<MailReviewSegment> segments) {
        return new LlmMailReviewRequest(segments.stream()
                .map(LlmMailReviewSegment::from)
                .toList());
    }
}
