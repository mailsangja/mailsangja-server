package com.mailsangja.core.common.exception.masking;

import com.mailsangja.core.common.exception.BaseException;

public class MaskingException extends BaseException {

    public MaskingException(MaskingErrorCode errorCode) {
        super(errorCode);
    }

    public MaskingException(MaskingErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    public MaskingException(MaskingErrorCode errorCode, Throwable cause) {
        super(errorCode);
        initCause(cause);
    }
}
