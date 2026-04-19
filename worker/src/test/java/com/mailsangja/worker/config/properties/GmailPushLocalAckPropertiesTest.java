package com.mailsangja.worker.config.properties;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailPushLocalAckPropertiesTest {

    @Test
    void isWhitelisted_설정된에러코드면true를반환한다() {
        GmailPushLocalAckProperties properties = new GmailPushLocalAckProperties();
        properties.setEnabled(true);
        properties.setWhitelistedErrorCodes(List.of("MAIL_ACCOUNT_NOT_FOUND", "INVALID_PUBSUB_OIDC_TOKEN"));
        properties.validate();

        assertTrue(properties.isWhitelisted(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND));
        assertTrue(properties.isWhitelisted(MailPushErrorCode.INVALID_PUBSUB_OIDC_TOKEN));
        assertFalse(properties.isWhitelisted(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION));
    }

    @Test
    void validate_잘못된에러코드가있으면예외가발생한다() {
        GmailPushLocalAckProperties properties = new GmailPushLocalAckProperties();
        properties.setEnabled(true);
        properties.setWhitelistedErrorCodes(List.of("NOT_EXISTS"));

        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void validate_enabled가false면잘못된에러코드가있어도예외가발생하지않는다() {
        GmailPushLocalAckProperties properties = new GmailPushLocalAckProperties();
        properties.setEnabled(false);
        properties.setWhitelistedErrorCodes(List.of("NOT_EXISTS"));

        properties.validate();

        assertFalse(properties.isWhitelisted(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND));
    }
}
