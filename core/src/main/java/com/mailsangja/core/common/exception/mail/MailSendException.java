package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.BaseException;

public class MailSendException extends BaseException {

    public MailSendException(MailSendErrorCode errorCode) {
        super(errorCode);
    }
}
