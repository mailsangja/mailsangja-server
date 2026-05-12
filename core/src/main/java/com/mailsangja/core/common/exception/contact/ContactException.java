package com.mailsangja.core.common.exception.contact;

import com.mailsangja.core.common.exception.BaseException;

public class ContactException extends BaseException {

    public ContactException(ContactErrorCode errorCode) {
        super(errorCode);
    }
}
