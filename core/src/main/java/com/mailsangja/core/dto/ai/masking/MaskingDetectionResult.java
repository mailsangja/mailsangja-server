package com.mailsangja.core.dto.ai.masking;

import com.mailsangja.core.common.exception.masking.MaskingErrorCode;
import com.mailsangja.core.common.exception.masking.MaskingException;

public record MaskingDetectionResult(
        PiiType piiType,
        int startInclusive,
        int endExclusive
) {

    public MaskingDetectionResult {
        if (piiType == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_TOKEN_TYPE);
        }
        if (startInclusive < 0 || endExclusive <= startInclusive) {
            throw new MaskingException(MaskingErrorCode.INVALID_DETECTION_RANGE);
        }
    }

    public int length() {
        return endExclusive - startInclusive;
    }

    public boolean overlaps(MaskingDetectionResult other) {
        return startInclusive < other.endExclusive && other.startInclusive < endExclusive;
    }
}
