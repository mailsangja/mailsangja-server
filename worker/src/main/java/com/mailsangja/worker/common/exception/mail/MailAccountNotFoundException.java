package com.mailsangja.worker.common.exception.mail;

public class MailAccountNotFoundException extends MailPushException {

    public MailAccountNotFoundException() {
        super(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND);
    }
}
