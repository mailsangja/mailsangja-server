package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.BaseException;

public class MailReviewException extends BaseException {

    public MailReviewException(MailReviewErrorCode errorCode) {
        super(errorCode);
    }

    public MailReviewException(MailReviewErrorCode errorCode, Throwable cause) {
        super(errorCode, cause.getMessage());
        initCause(cause);
    }
}
