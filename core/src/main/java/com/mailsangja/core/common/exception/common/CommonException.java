package com.mailsangja.core.common.exception.common;

import com.mailsangja.core.common.exception.BaseException;

public class CommonException extends BaseException {

    public CommonException(CommonErrorCode errorCode) {
        super(errorCode);
    }

    public CommonException(CommonErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
