package com.mailsangja.core.common.exception;

import com.mailsangja.core.common.exception.common.CommonErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse(CommonErrorCode.INVALID_REQUEST.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, CommonErrorCode.INVALID_REQUEST.getCode(), message));
    }

    @ExceptionHandler(LabelException.class)
    public ResponseEntity<ErrorResponse> handleLabelException(LabelException e) {
        log.warn("LabelException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.withRetryAfter(e.getErrorCode(), e.getRetryAfterSeconds()));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        log.warn("BaseException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.from(e.getErrorCode()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        if ("/favicon.ico".equals(e.getResourcePath())) {
            return ResponseEntity.noContent().build();
        }

        log.warn("No static resource found for request '{}'", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception e) {
        log.error("[Exception] Unhandled Server Error - {}", e.getMessage(), e);
        return ResponseEntity.status(CommonErrorCode.INTERNAL_FAILURE.getStatus())
                .body(ErrorResponse.from(CommonErrorCode.INTERNAL_FAILURE));
    }
}
