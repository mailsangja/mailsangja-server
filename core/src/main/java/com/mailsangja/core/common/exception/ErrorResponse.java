package com.mailsangja.core.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String code, String message, Long retryAfterSeconds) {

    public ErrorResponse(int status, String code, String message) {
        this(status, code, message, null);
    }

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ErrorResponse withRetryAfter(ErrorCode errorCode, Long retryAfterSeconds) {
        return new ErrorResponse(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage(),
                retryAfterSeconds
        );
    }
}
