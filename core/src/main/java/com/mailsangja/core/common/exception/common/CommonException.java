package com.mailsangja.core.common.exception.common;

import com.example.ajouevent_be_v2.common.exception.AjouBaseException;

// common 레이어(util, 인프라 연동 등)에서 발생하는 공통 예외
public class CommonException extends AjouBaseException {

    public CommonException(CommonErrorCode errorCode) {
        super(errorCode);
    }

    public CommonException(CommonErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
