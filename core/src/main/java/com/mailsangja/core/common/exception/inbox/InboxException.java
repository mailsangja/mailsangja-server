package com.mailsangja.core.common.exception.inbox;

import com.mailsangja.core.common.exception.BaseException;

public class InboxException extends BaseException {

    public InboxException(InboxErrorCode errorCode) {
        super(errorCode);
    }
}
