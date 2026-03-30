package com.mailsangja.core.common.exception.user;

import com.mailsangja.core.common.exception.BaseException;

public class UserException extends BaseException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
