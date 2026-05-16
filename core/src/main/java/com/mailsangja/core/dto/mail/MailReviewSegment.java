package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailReviewErrorCode;
import com.mailsangja.core.common.exception.mail.MailReviewException;

public record MailReviewSegment(
        String segmentId,
        MailReviewField field,
        int index,
        String hash,
        String text,
        int globalStartOffset,
        int globalEndOffset
) {

    public MailReviewSegment {
        if (segmentId == null || segmentId.isBlank() || field == null || hash == null || hash.isBlank()
                || text == null || text.isBlank() || globalStartOffset < 0 || globalEndOffset < globalStartOffset) {
            throw new MailReviewException(MailReviewErrorCode.INVALID_REQUEST);
        }
    }
}
