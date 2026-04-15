package com.mailsangja.core.common.exception.trash;

import com.mailsangja.core.common.exception.BaseException;

public class TrashException extends BaseException {

    public TrashException(TrashErrorCode errorCode) {
        super(errorCode);
    }
}
