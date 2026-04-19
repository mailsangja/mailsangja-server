package com.mailsangja.worker.common.exception;

import com.mailsangja.worker.common.exception.common.CommonErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GmailPushLocalAckProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String GMAIL_PUSH_PATH = "/api/v1/gmail/push";

    private final GmailPushLocalAckProperties gmailPushLocalAckProperties;

    @ExceptionHandler(MailPushException.class)
    public ResponseEntity<?> handleMailPushException(MailPushException e, HttpServletRequest request) {
        MailPushErrorCode errorCode = (MailPushErrorCode) e.getErrorCode();
        if (shouldAckLocalGmailPush(errorCode, request)) {
            log.warn(
                    "Local Gmail push ack applied: path='{}', errorCode={}, message={}",
                    request.getRequestURI(),
                    errorCode.name(),
                    e.getMessage()
            );
            return ResponseEntity.noContent().build();
        }

        log.warn("BaseException: {}", e.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        log.warn("BaseException: {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
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

    private boolean shouldAckLocalGmailPush(MailPushErrorCode errorCode, HttpServletRequest request) {
        return request != null
                && GMAIL_PUSH_PATH.equals(request.getRequestURI())
                && gmailPushLocalAckProperties.isWhitelisted(errorCode);
    }
}
