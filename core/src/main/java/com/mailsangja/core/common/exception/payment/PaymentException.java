package com.mailsangja.core.common.exception.payment;

import com.mailsangja.core.common.exception.BaseException;

public class PaymentException extends BaseException {

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentException(PaymentErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
