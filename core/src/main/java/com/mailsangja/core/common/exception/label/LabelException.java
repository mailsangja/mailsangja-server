package com.mailsangja.core.common.exception.label;

import com.mailsangja.core.common.exception.BaseException;
import lombok.Getter;

@Getter
public class LabelException extends BaseException {

    private final Long retryAfterSeconds;

    public LabelException(LabelErrorCode errorCode) {
        super(errorCode);
        this.retryAfterSeconds = null;
    }

    public LabelException(LabelErrorCode errorCode, long retryAfterSeconds) {
        super(errorCode);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
