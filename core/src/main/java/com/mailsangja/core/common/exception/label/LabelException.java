package com.mailsangja.core.common.exception.label;

import com.mailsangja.core.common.exception.BaseException;

public class LabelException extends BaseException {

    public LabelException(LabelErrorCode errorCode) {
        super(errorCode);
    }
}
