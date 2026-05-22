package com.mailsangja.core.common.exception.ai;

import com.mailsangja.core.common.exception.BaseException;

public class AiPlaygroundException extends BaseException {

    public AiPlaygroundException(AiPlaygroundErrorCode errorCode) {
        super(errorCode);
    }

    public AiPlaygroundException(AiPlaygroundErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
