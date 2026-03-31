package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.BaseException;

public class MailAccountException extends BaseException {

    public MailAccountException(MailAccountErrorCode errorCode) {
        super(errorCode);
    }
}
