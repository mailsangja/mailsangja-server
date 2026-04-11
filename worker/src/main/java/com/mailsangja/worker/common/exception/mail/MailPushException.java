package com.mailsangja.worker.common.exception.mail;

import com.mailsangja.worker.common.exception.BaseException;

public class MailPushException extends BaseException {

    public MailPushException(MailPushErrorCode errorCode) {
        super(errorCode);
    }
}
