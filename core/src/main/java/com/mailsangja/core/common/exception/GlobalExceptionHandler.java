package com.mailsangja.core.common.exception;

import com.mailsangja.core.common.exception.common.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        log.warn("BaseException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.from(e.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception e) {
        log.error("[Exception] Unhandled Server Error - {}", e.getMessage(), e);
        return ResponseEntity.status(CommonErrorCode.INTERNAL_FAILURE.getStatus())
                .body(ErrorResponse.from(CommonErrorCode.INTERNAL_FAILURE));
    }
}
