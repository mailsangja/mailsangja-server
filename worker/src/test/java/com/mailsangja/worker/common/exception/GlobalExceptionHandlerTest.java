package com.mailsangja.worker.common.exception;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GmailPushLocalAckProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GlobalExceptionHandlerTest {

    @Test
    void handleMailPushException_화이트리스트에포함된지메일푸시예외면204를반환한다() {
        GmailPushLocalAckProperties properties = new GmailPushLocalAckProperties();
        properties.setEnabled(true);
        properties.setWhitelistedErrorCodes(List.of("MAIL_ACCOUNT_NOT_FOUND"));
        properties.validate();

        GlobalExceptionHandler handler = new GlobalExceptionHandler(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/gmail/push");

        ResponseEntity<?> response = handler.handleMailPushException(
                new MailPushException(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND),
                request
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void handleMailPushException_화이트리스트에없으면원래에러응답을반환한다() {
        GmailPushLocalAckProperties properties = new GmailPushLocalAckProperties();
        properties.setEnabled(true);
        properties.setWhitelistedErrorCodes(List.of("MAIL_ACCOUNT_NOT_FOUND"));
        properties.validate();

        GlobalExceptionHandler handler = new GlobalExceptionHandler(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/gmail/push");

        ResponseEntity<?> response = handler.handleMailPushException(
                new MailPushException(MailPushErrorCode.INVALID_PUBSUB_OIDC_TOKEN),
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }

    @Test
    void handleMailPushException_다른경로면화이트리스트여도원래에러응답을반환한다() {
        GmailPushLocalAckProperties properties = new GmailPushLocalAckProperties();
        properties.setEnabled(true);
        properties.setWhitelistedErrorCodes(List.of("MAIL_ACCOUNT_NOT_FOUND"));
        properties.validate();

        GlobalExceptionHandler handler = new GlobalExceptionHandler(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/other");

        ResponseEntity<?> response = handler.handleMailPushException(
                new MailPushException(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }
}
