package com.mailsangja.worker.dto.ai.masking;

import com.mailsangja.worker.common.exception.masking.MaskingErrorCode;
import com.mailsangja.worker.common.exception.masking.MaskingException;

public record MaskingTokenResult(
        PiiType piiType,
        String token,
        String originalValue,
        int startInclusive,
        int endExclusive
) {

    public MaskingTokenResult {
        if (piiType == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_TOKEN_TYPE);
        }
        if (token == null || token.isBlank()) {
            throw new MaskingException(MaskingErrorCode.INVALID_TOKEN);
        }
        if (originalValue == null || originalValue.isBlank()) {
            throw new MaskingException(MaskingErrorCode.INVALID_ORIGINAL_VALUE);
        }
        if (startInclusive < 0 || endExclusive <= startInclusive) {
            throw new MaskingException(MaskingErrorCode.INVALID_TOKEN_RANGE);
        }
    }
}
