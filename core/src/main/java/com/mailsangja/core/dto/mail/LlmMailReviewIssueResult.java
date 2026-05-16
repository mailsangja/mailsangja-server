package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailReviewErrorCode;
import com.mailsangja.core.common.exception.mail.MailReviewException;

public record LlmMailReviewIssueResult(
        String segmentId,
        MailReviewIssueType type,
        MailReviewSeverity severity,
        String originalText,
        String replacementText,
        String contextBefore,
        String contextAfter,
        String reason
) {

    public LlmMailReviewIssueResult {
        if (segmentId == null || segmentId.isBlank() || originalText == null || originalText.isBlank()
                || replacementText == null || replacementText.isBlank()) {
            throw new MailReviewException(MailReviewErrorCode.AI_RESPONSE_INVALID);
        }
        type = type == null ? MailReviewIssueType.CONTEXT : type;
        severity = severity == null ? MailReviewSeverity.LOW : severity;
        contextBefore = nullToEmpty(contextBefore);
        contextAfter = nullToEmpty(contextAfter);
        reason = nullToEmpty(reason);
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
