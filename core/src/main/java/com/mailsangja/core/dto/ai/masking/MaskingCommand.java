package com.mailsangja.core.dto.ai.masking;

import com.mailsangja.core.common.exception.masking.MaskingErrorCode;
import com.mailsangja.core.common.exception.masking.MaskingException;

import java.util.EnumSet;
import java.util.Set;

public record MaskingCommand(
        MaskingScope scope,
        Set<PiiType> enabledTypes
) {

    private static final Set<PiiType> DEFAULT_ENABLED_TYPES = EnumSet.complementOf(EnumSet.of(
            PiiType.EMAIL,
            PiiType.PERSON_NAME
    ));

    public MaskingCommand {
        if (scope == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_SCOPE);
        }
        if (hasNullEnabledType(enabledTypes)) {
            throw new MaskingException(MaskingErrorCode.INVALID_TOKEN_TYPE);
        }
        enabledTypes = enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.copyOf(DEFAULT_ENABLED_TYPES)
                : EnumSet.copyOf(enabledTypes);
    }

    public static MaskingCommand currentContext() {
        return new MaskingCommand(MaskingScope.CURRENT_CONTEXT, EnumSet.copyOf(DEFAULT_ENABLED_TYPES));
    }

    public static MaskingCommand pastContext() {
        return new MaskingCommand(MaskingScope.PAST_CONTEXT, EnumSet.copyOf(DEFAULT_ENABLED_TYPES));
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
