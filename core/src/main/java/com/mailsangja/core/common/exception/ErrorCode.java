package com.mailsangja.core.common.exception;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessage();
}
