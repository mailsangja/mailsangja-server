package com.mailsangja.core.dto.ai.masking;

import com.mailsangja.core.common.exception.masking.MaskingErrorCode;
import com.mailsangja.core.common.exception.masking.MaskingException;

import java.util.List;
import java.util.Map;

public record MaskingResult(
        String maskedText,
        List<MaskingTokenResult> tokens,
        Map<String, String> restoreTokenMap,
        Map<String, String> redactedTokenMap
) {

    public MaskingResult {
        if (maskedText == null || tokens == null || restoreTokenMap == null || redactedTokenMap == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_RESULT);
        }
        if (hasNullToken(tokens) || hasNullEntry(restoreTokenMap) || hasNullEntry(redactedTokenMap)) {
            throw new MaskingException(MaskingErrorCode.INVALID_RESULT);
        }
        tokens = List.copyOf(tokens);
        restoreTokenMap = Map.copyOf(restoreTokenMap);
        redactedTokenMap = Map.copyOf(redactedTokenMap);
    }

    private static boolean hasNullToken(List<MaskingTokenResult> tokens) {
        for (MaskingTokenResult token : tokens) {
            if (token == null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNullEntry(Map<String, String> tokenMap) {
        return tokenMap.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null);
    }
}
