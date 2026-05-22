package com.mailsangja.core.common.exception.ai;

import com.mailsangja.core.common.exception.BaseException;

public class AiModelException extends BaseException {

    public AiModelException(AiModelErrorCode errorCode) {
        super(errorCode);
    }
}
