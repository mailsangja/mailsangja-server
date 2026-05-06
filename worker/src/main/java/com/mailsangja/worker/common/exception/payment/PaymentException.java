package com.mailsangja.worker.common.exception.payment;

import com.mailsangja.worker.common.exception.BaseException;

public class PaymentException extends BaseException {

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentException(PaymentErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
