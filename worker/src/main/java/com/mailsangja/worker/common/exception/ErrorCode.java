package com.mailsangja.worker.common.exception;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessage();
}
