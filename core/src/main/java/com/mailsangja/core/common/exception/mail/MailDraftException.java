package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.BaseException;

public class MailDraftException extends BaseException {

    public MailDraftException(MailDraftErrorCode errorCode) {
        super(errorCode);
    }
}
