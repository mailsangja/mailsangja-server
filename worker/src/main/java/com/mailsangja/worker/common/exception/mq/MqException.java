package com.mailsangja.worker.common.exception.mq;

import com.mailsangja.worker.common.exception.BaseException;

public class MqException extends BaseException {

    public MqException(MqErrorCode errorCode) {
        super(errorCode);
    }

    public MqException(MqErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
