package com.mailsangja.core.common.exception.mq;

import com.mailsangja.core.common.exception.BaseException;

public class MqException extends BaseException {

    public MqException(MqErrorCode errorCode) {
        super(errorCode);
    }

    public MqException(MqErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
