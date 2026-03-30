package com.mailsangja.core.common.exception.auth;

import com.mailsangja.core.common.exception.BaseException;

public class AuthException extends BaseException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
