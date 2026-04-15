package com.mailsangja.core.common.exception.fcm;

import com.mailsangja.core.common.exception.BaseException;

public class FcmTokenException extends BaseException {

    public FcmTokenException(FcmTokenErrorCode errorCode) {
        super(errorCode);
    }
}
