package com.mailsangja.core.dto.ai.masking;

import com.mailsangja.core.common.exception.masking.MaskingErrorCode;
import com.mailsangja.core.common.exception.masking.MaskingException;

import java.util.EnumSet;
import java.util.Set;

public record MaskingCommand(
        MaskingScope scope,
        Set<PiiType> enabledTypes
) {

    public MaskingCommand {
        if (scope == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_SCOPE);
        }
        if (hasNullEnabledType(enabledTypes)) {
            throw new MaskingException(MaskingErrorCode.INVALID_TOKEN_TYPE);
        }
        enabledTypes = enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.allOf(PiiType.class)
                : EnumSet.copyOf(enabledTypes);
    }

    public static MaskingCommand currentContext() {
        return new MaskingCommand(MaskingScope.CURRENT_CONTEXT, EnumSet.allOf(PiiType.class));
    }

    public static MaskingCommand pastContext() {
        return new MaskingCommand(MaskingScope.PAST_CONTEXT, EnumSet.allOf(PiiType.class));
    }

    public boolean isEnabled(PiiType piiType) {
        return enabledTypes.contains(piiType);
    }

    private static boolean hasNullEnabledType(Set<PiiType> enabledTypes) {
        if (enabledTypes == null) {
            return false;
        }
        for (PiiType enabledType : enabledTypes) {
            if (enabledType == null) {
                return true;
            }
        }
        return false;
    }
}
